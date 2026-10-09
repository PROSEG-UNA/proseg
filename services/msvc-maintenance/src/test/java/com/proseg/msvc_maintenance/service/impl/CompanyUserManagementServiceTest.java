package com.proseg.msvc_maintenance.service.impl;

import com.proseg.common.api.response.ApiResponse;
import com.proseg.common.kafka.UserAssignedDomainEvent;
import com.proseg.msvc_maintenance.client.AuthClient;
import com.proseg.msvc_maintenance.dto.response.KeycloakUserResponse;
import com.proseg.msvc_maintenance.entity.Company;
import com.proseg.msvc_maintenance.entity.UserCompany;
import com.proseg.msvc_maintenance.exception.CompanyException;
import com.proseg.msvc_maintenance.exception.UserCompanyException;
import com.proseg.msvc_maintenance.repository.CompanyRepository;
import com.proseg.msvc_maintenance.repository.UserCompanyRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyUserManagementServiceTest {

    private static final String KEYCLOAK_USER_A = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    private static final String KEYCLOAK_USER_B = "b2c3d4e5-f6a7-8901-bcde-f12345678901";

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserCompanyRepository userCompanyRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private AuthClient authClient;

    @InjectMocks
    private CompanyUserManagementService service;

    @Captor
    private ArgumentCaptor<UserCompany> userCompanyCaptor;
    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    private UUID companyId;
    private Company company;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        company = Company.builder().id(companyId).name("Acme SA").build();
    }

    @Test
    @DisplayName("assignUserToCompany con empresa inexistente lanza CompanyException.notFound")
    void assignUserToCompany_cuandoEmpresaNoExiste_lanzaNotFound() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignUserToCompany(companyId, KEYCLOAK_USER_A))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> assertThat(((CompanyException) ex).getErrorCode()).isEqualTo("COMPANY_NOT_FOUND"));

        verifyNoInteractions(authClient);
        verify(userCompanyRepository, never()).save(any(UserCompany.class));
    }

    @Test
    @DisplayName("assignUserToCompany cuando Keycloak no devuelve usuario lanza invalidKeycloakUser")
    void assignUserToCompany_cuandoUsuarioNoExisteEnKeycloak_lanzaInvalidKeycloakUser() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(authClient.findUserByKeycloakId(KEYCLOAK_USER_A)).thenReturn(new ApiResponse<>("ok", null, 200));

        assertThatThrownBy(() -> service.assignUserToCompany(companyId, KEYCLOAK_USER_A))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> {
                    CompanyException ce = (CompanyException) ex;
                    assertThat(ce.getErrorCode()).isEqualTo("INVALID_KEYCLOAK_USER");
                    assertThat(ce.getMessage())
                            .isEqualTo("No existe un usuario en Keycloak con el id: " + KEYCLOAK_USER_A);
                });

        verify(userCompanyRepository, never()).save(any(UserCompany.class));
    }

    @Test
    @DisplayName("assignUserToCompany no impide asignar a otra empresa si ya está en una distinta")
    void assignUserToCompany_cuandoUsuarioAsignadoOtraEmpresa_creaNuevaRelacion() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(authClient.findUserByKeycloakId(KEYCLOAK_USER_A))
                .thenReturn(apiUser(KEYCLOAK_USER_A, "user@acme.com"));
        when(userCompanyRepository.findByCompanyIdAndKeycloakUserIdIncludingDeleted(companyId, KEYCLOAK_USER_A))
                .thenReturn(Optional.empty());
        when(userCompanyRepository.save(any(UserCompany.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userCompanyRepository.findAllByCompanyId(companyId)).thenReturn(List.of());

        UserCompany result = service.assignUserToCompany(companyId, KEYCLOAK_USER_A);

        verify(userCompanyRepository).save(userCompanyCaptor.capture());
        UserCompany saved = userCompanyCaptor.getValue();
        assertThat(saved.getKeycloakUserId()).isEqualTo(KEYCLOAK_USER_A);
        assertThat(saved.getUserEmail()).isEqualTo("user@acme.com");
        assertThat(saved.getCompany()).isSameAs(company);
        assertThat(result).isSameAs(saved);

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        UserAssignedDomainEvent event = (UserAssignedDomainEvent) eventCaptor.getValue();
        assertThat(event.companyId()).isEqualTo(companyId);
        assertThat(event.keycloakUserIds()).containsExactly(KEYCLOAK_USER_A);
    }

    @Test
    @DisplayName("assignUserToCompany con relación activa en la misma empresa lanza duplicateRelation")
    void assignUserToCompany_cuandoYaAsignadoMismaEmpresa_lanzaDuplicateRelation() {
        UserCompany existing = UserCompany.builder()
                .company(company)
                .keycloakUserId(KEYCLOAK_USER_A)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(authClient.findUserByKeycloakId(KEYCLOAK_USER_A))
                .thenReturn(apiUser(KEYCLOAK_USER_A, "user@acme.com"));
        when(userCompanyRepository.findByCompanyIdAndKeycloakUserIdIncludingDeleted(companyId, KEYCLOAK_USER_A))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.assignUserToCompany(companyId, KEYCLOAK_USER_A))
                .isInstanceOf(UserCompanyException.class)
                .satisfies(ex -> {
                    UserCompanyException uce = (UserCompanyException) ex;
                    assertThat(uce.getErrorCode()).isEqualTo("USER_COMPANY_DUPLICATE");
                    assertThat(uce.getMessage()).isEqualTo("Este usuario ya tiene asignada esa empresa.");
                });

        verify(userCompanyRepository, never()).save(any(UserCompany.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("assignUserToCompany reactiva relación soft-deleted en la misma empresa y publica evento")
    void assignUserToCompany_cuandoRelacionEliminadaMismaEmpresa_reactivaYPublica() {
        UserCompany deleted = UserCompany.builder()
                .company(company)
                .keycloakUserId(KEYCLOAK_USER_A)
                .userEmail("old@acme.com")
                .build();
        deleted.markAsDeleted();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(authClient.findUserByKeycloakId(KEYCLOAK_USER_A))
                .thenReturn(apiUser(KEYCLOAK_USER_A, "new@acme.com"));
        when(userCompanyRepository.findByCompanyIdAndKeycloakUserIdIncludingDeleted(companyId, KEYCLOAK_USER_A))
                .thenReturn(Optional.of(deleted));
        when(userCompanyRepository.save(deleted)).thenReturn(deleted);
        when(userCompanyRepository.findAllByCompanyId(companyId)).thenReturn(List.of(deleted));

        UserCompany result = service.assignUserToCompany(companyId, KEYCLOAK_USER_A);

        assertThat(deleted.isDeleted()).isFalse();
        assertThat(deleted.getUserEmail()).isEqualTo("new@acme.com");
        verify(userCompanyRepository).save(deleted);
        assertThat(result).isSameAs(deleted);

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        UserAssignedDomainEvent event = (UserAssignedDomainEvent) eventCaptor.getValue();
        assertThat(event.keycloakUserIds()).containsExactly(KEYCLOAK_USER_A);
    }

    @Test
    @DisplayName("assignUserToCompany camino feliz persiste relación y publica UserAssignedDomainEvent")
    void assignUserToCompany_cuandoValido_guardaYPublicaEvento() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(authClient.findUserByKeycloakId(KEYCLOAK_USER_A))
                .thenReturn(apiUser(KEYCLOAK_USER_A, "user@acme.com"));
        when(userCompanyRepository.findByCompanyIdAndKeycloakUserIdIncludingDeleted(companyId, KEYCLOAK_USER_A))
                .thenReturn(Optional.empty());
        when(userCompanyRepository.save(any(UserCompany.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userCompanyRepository.findAllByCompanyId(companyId)).thenReturn(List.of());

        service.assignUserToCompany(companyId, KEYCLOAK_USER_A);

        verify(userCompanyRepository).save(userCompanyCaptor.capture());
        assertThat(userCompanyCaptor.getValue().getKeycloakUserId()).isEqualTo(KEYCLOAK_USER_A);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(((UserAssignedDomainEvent) eventCaptor.getValue()).companyId()).isEqualTo(companyId);
    }

    @Test
    @DisplayName("assignUsersToCompany con lista null no crea relaciones ni publica eventos")
    void assignUsersToCompany_cuandoListaNull_noHaceCambios() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userCompanyRepository.findAllByCompanyId(companyId)).thenReturn(List.of());

        List<UserCompany> created = service.assignUsersToCompany(companyId, null);

        assertThat(created).isEmpty();
        verify(userCompanyRepository, never()).save(any(UserCompany.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("assignUsersToCompany con lista vacía no crea relaciones ni publica eventos")
    void assignUsersToCompany_cuandoListaVacia_noHaceCambios() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userCompanyRepository.findAllByCompanyId(companyId)).thenReturn(List.of());

        List<UserCompany> created = service.assignUsersToCompany(companyId, List.of());

        assertThat(created).isEmpty();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("assignUsersToCompany normaliza ids en blanco y duplicados")
    void assignUsersToCompany_cuandoIdsBlancosODuplicados_soloProcesaUnicosValidos() {
        List<String> raw = new ArrayList<>();
        raw.add("  " + KEYCLOAK_USER_A + "  ");
        raw.add("");
        raw.add(null);
        raw.add(KEYCLOAK_USER_A);
        raw.add(KEYCLOAK_USER_B);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userCompanyRepository.findByCompanyIdAndKeycloakUserIdIncludingDeleted(companyId, KEYCLOAK_USER_A))
                .thenReturn(Optional.empty());
        when(userCompanyRepository.findByCompanyIdAndKeycloakUserIdIncludingDeleted(companyId, KEYCLOAK_USER_B))
                .thenReturn(Optional.empty());
        when(authClient.findUserByKeycloakId(KEYCLOAK_USER_A)).thenReturn(apiUser(KEYCLOAK_USER_A, "a@acme.com"));
        when(authClient.findUserByKeycloakId(KEYCLOAK_USER_B)).thenReturn(apiUser(KEYCLOAK_USER_B, "b@acme.com"));
        when(userCompanyRepository.save(any(UserCompany.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userCompanyRepository.findAllByCompanyId(companyId)).thenReturn(List.of());

        List<UserCompany> created = service.assignUsersToCompany(companyId, raw);

        assertThat(created).hasSize(2);
        verify(userCompanyRepository, times(2)).save(userCompanyCaptor.capture());
        assertThat(userCompanyCaptor.getAllValues())
                .extracting(UserCompany::getKeycloakUserId)
                .containsExactly(KEYCLOAK_USER_A, KEYCLOAK_USER_B);

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        UserAssignedDomainEvent event = (UserAssignedDomainEvent) eventCaptor.getValue();
        assertThat(event.keycloakUserIds()).containsExactly(KEYCLOAK_USER_A, KEYCLOAK_USER_B);
    }

    @Test
    @DisplayName("assignUsersToCompany omite activos, reactiva eliminados y crea nuevos")
    void assignUsersToCompany_cuandoMezclaEstados_soloAgregaNuevosYReactivados() {
        UserCompany active = UserCompany.builder().company(company).keycloakUserId(KEYCLOAK_USER_A).build();
        UserCompany softDeleted = UserCompany.builder().company(company).keycloakUserId(KEYCLOAK_USER_B).build();
        softDeleted.markAsDeleted();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userCompanyRepository.findByCompanyIdAndKeycloakUserIdIncludingDeleted(companyId, KEYCLOAK_USER_A))
                .thenReturn(Optional.of(active));
        when(userCompanyRepository.findByCompanyIdAndKeycloakUserIdIncludingDeleted(companyId, KEYCLOAK_USER_B))
                .thenReturn(Optional.of(softDeleted));
        String keycloakUserC = "c3d4e5f6-a7b8-9012-cdef-123456789012";
        when(userCompanyRepository.findByCompanyIdAndKeycloakUserIdIncludingDeleted(companyId, keycloakUserC))
                .thenReturn(Optional.empty());

        when(authClient.findUserByKeycloakId(KEYCLOAK_USER_B)).thenReturn(apiUser(KEYCLOAK_USER_B, "b@acme.com"));
        when(authClient.findUserByKeycloakId(keycloakUserC)).thenReturn(apiUser(keycloakUserC, "c@acme.com"));
        when(userCompanyRepository.save(any(UserCompany.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userCompanyRepository.findAllByCompanyId(companyId)).thenReturn(List.of());

        List<UserCompany> created = service.assignUsersToCompany(
                companyId,
                List.of(KEYCLOAK_USER_A, KEYCLOAK_USER_B, keycloakUserC));

        assertThat(created).hasSize(2);
        assertThat(softDeleted.isDeleted()).isFalse();
        verify(userCompanyRepository, never()).save(active);
        verify(userCompanyRepository, times(2)).save(userCompanyCaptor.capture());

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        UserAssignedDomainEvent event = (UserAssignedDomainEvent) eventCaptor.getValue();
        assertThat(event.keycloakUserIds()).containsExactly(KEYCLOAK_USER_B, keycloakUserC);
    }

    @Test
    @DisplayName("removeUserFromCompany existente elimina la relación")
    void removeUserFromCompany_cuandoExiste_eliminaRelacion() {
        UserCompany link = UserCompany.builder().company(company).keycloakUserId(KEYCLOAK_USER_A).build();
        when(userCompanyRepository.findByCompanyIdAndKeycloakUserId(companyId, KEYCLOAK_USER_A))
                .thenReturn(Optional.of(link));

        service.removeUserFromCompany(companyId, KEYCLOAK_USER_A);

        verify(userCompanyRepository).delete(link);
    }

    @Test
    @DisplayName("removeUserFromCompany inexistente lanza notFound")
    void removeUserFromCompany_cuandoNoExiste_lanzaNotFound() {
        when(userCompanyRepository.findByCompanyIdAndKeycloakUserId(companyId, KEYCLOAK_USER_A))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeUserFromCompany(companyId, KEYCLOAK_USER_A))
                .isInstanceOf(UserCompanyException.class)
                .satisfies(ex -> assertThat(((UserCompanyException) ex).getErrorCode()).isEqualTo("USER_COMPANY_NOT_FOUND"));

        verify(userCompanyRepository, never()).delete(any(UserCompany.class));
    }

    @Test
    @DisplayName("syncUsers con null devuelve relaciones actuales sin modificar")
    void syncUsers_cuandoRequestedNull_devuelveActualesSinBorrar() {
        UserCompany existing = UserCompany.builder().company(company).keycloakUserId(KEYCLOAK_USER_A).build();
        when(userCompanyRepository.findAllByCompanyId(companyId)).thenReturn(List.of(existing));

        List<UserCompany> result = service.syncUsers(company, null);

        assertThat(result).containsExactly(existing);
        verify(userCompanyRepository, never()).save(any(UserCompany.class));
        verify(userCompanyRepository, never()).delete(any(UserCompany.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("syncUsers agrega nuevos, conserva existentes y elimina los que ya no están")
    void syncUsers_cuandoListaCambia_sincronizaRelaciones() {
        UserCompany keep = UserCompany.builder().company(company).keycloakUserId(KEYCLOAK_USER_A).build();
        UserCompany remove = UserCompany.builder().company(company).keycloakUserId("d4e5f6a7-b8c9-0123-def0-234567890123").build();
        String newUserId = KEYCLOAK_USER_B;

        when(userCompanyRepository.findAllByCompanyId(companyId)).thenReturn(List.of(keep, remove));
        when(authClient.findUserByKeycloakId(newUserId)).thenReturn(apiUser(newUserId, "b@acme.com"));
        when(userCompanyRepository.save(any(UserCompany.class))).thenAnswer(inv -> inv.getArgument(0));

        List<UserCompany> result = service.syncUsers(company, List.of(KEYCLOAK_USER_A, newUserId));

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isSameAs(keep);
        verify(userCompanyRepository).save(userCompanyCaptor.capture());
        assertThat(userCompanyCaptor.getValue().getKeycloakUserId()).isEqualTo(newUserId);
        verify(userCompanyRepository).delete(remove);

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        UserAssignedDomainEvent event = (UserAssignedDomainEvent) eventCaptor.getValue();
        assertThat(event.keycloakUserIds()).containsExactly(newUserId);
    }

    @Test
    @DisplayName("syncUsers con lista vacía elimina todas las relaciones actuales")
    void syncUsers_cuandoListaVacia_eliminaTodasLasRelaciones() {
        UserCompany one = UserCompany.builder().company(company).keycloakUserId(KEYCLOAK_USER_A).build();
        UserCompany two = UserCompany.builder().company(company).keycloakUserId(KEYCLOAK_USER_B).build();
        when(userCompanyRepository.findAllByCompanyId(companyId)).thenReturn(List.of(one, two));

        List<UserCompany> result = service.syncUsers(company, List.of());

        assertThat(result).isEmpty();
        verify(userCompanyRepository).delete(one);
        verify(userCompanyRepository).delete(two);
        verify(userCompanyRepository, never()).save(any(UserCompany.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("findByCompanyId exige empresa existente y delega al repositorio")
    void findByCompanyId_cuandoEmpresaExiste_retornaRelaciones() {
        UserCompany link = UserCompany.builder().keycloakUserId(KEYCLOAK_USER_A).build();
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userCompanyRepository.findAllByCompanyId(companyId)).thenReturn(List.of(link));

        assertThat(service.findByCompanyId(companyId)).containsExactly(link);
    }

    @Test
    @DisplayName("findUsersByCompanyId consulta Keycloak por cada id de la empresa")
    void findUsersByCompanyId_cuandoHayUsuarios_mapeaRespuestasAuth() {
        UserCompany link = UserCompany.builder().keycloakUserId(KEYCLOAK_USER_A).build();
        KeycloakUserResponse kcUser = keycloakUser(KEYCLOAK_USER_A, "user@acme.com");

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userCompanyRepository.findAllByCompanyId(companyId)).thenReturn(List.of(link));
        when(authClient.findUserByKeycloakId(KEYCLOAK_USER_A)).thenReturn(apiUser(KEYCLOAK_USER_A, "user@acme.com"));

        List<KeycloakUserResponse> users = service.findUsersByCompanyId(companyId);

        assertThat(users).containsExactly(kcUser);
    }

    @Test
    @DisplayName("findKeycloakUserIdsByCompanyId devuelve ids de las relaciones")
    void findKeycloakUserIdsByCompanyId_cuandoHayRelaciones_retornaIds() {
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userCompanyRepository.findAllByCompanyId(companyId)).thenReturn(List.of(
                UserCompany.builder().keycloakUserId(KEYCLOAK_USER_A).build(),
                UserCompany.builder().keycloakUserId(KEYCLOAK_USER_B).build()
        ));

        assertThat(service.findKeycloakUserIdsByCompanyId(companyId))
                .containsExactly(KEYCLOAK_USER_A, KEYCLOAK_USER_B);
    }

    @Test
    @DisplayName("findByCompanyIdAndKeycloakUserId delega al repositorio")
    void findByCompanyIdAndKeycloakUserId_cuandoExiste_retornaOptional() {
        UserCompany link = UserCompany.builder().keycloakUserId(KEYCLOAK_USER_A).build();
        when(userCompanyRepository.findByCompanyIdAndKeycloakUserId(companyId, KEYCLOAK_USER_A))
                .thenReturn(Optional.of(link));

        assertThat(service.findByCompanyIdAndKeycloakUserId(companyId, KEYCLOAK_USER_A))
                .contains(link);
    }

    @Test
    @DisplayName("assignUserToCompany con Feign NotFound lanza invalidKeycloakUser")
    void assignUserToCompany_cuandoKeycloak404_lanzaInvalidKeycloakUser() {
        FeignException.NotFound notFound = mock(FeignException.NotFound.class);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(authClient.findUserByKeycloakId(KEYCLOAK_USER_A)).thenThrow(notFound);

        assertThatThrownBy(() -> service.assignUserToCompany(companyId, KEYCLOAK_USER_A))
                .isInstanceOf(CompanyException.class)
                .satisfies(ex -> assertThat(((CompanyException) ex).getErrorCode()).isEqualTo("INVALID_KEYCLOAK_USER"));
    }

    private static ApiResponse<KeycloakUserResponse> apiUser(String id, String email) {
        return new ApiResponse<>("ok", keycloakUser(id, email), 200);
    }

    private static KeycloakUserResponse keycloakUser(String id, String email) {
        return new KeycloakUserResponse(id, "user", email, "Ana", "Perez", "ACTIVE", List.of());
    }
}
