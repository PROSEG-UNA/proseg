package com.proseg.msvc_maintenance.service.impl;

import com.proseg.msvc_maintenance.client.AuthClient;
import com.proseg.msvc_maintenance.client.InventoryClient;
import com.proseg.msvc_maintenance.config.MaintenanceNotificationProperties;
import com.proseg.msvc_maintenance.dto.request.TicketCreateRequestDto;
import com.proseg.msvc_maintenance.exception.TicketException;
import com.proseg.msvc_maintenance.repository.TicketAssetRepository;
import com.proseg.msvc_maintenance.repository.TicketCommentRepository;
import com.proseg.msvc_maintenance.repository.TicketPhotoRepository;
import com.proseg.msvc_maintenance.repository.TicketRepository;
import com.proseg.msvc_maintenance.repository.UserCompanyRepository;
import com.proseg.msvc_maintenance.security.Privileges;
import com.proseg.msvc_maintenance.websocket.TicketWebSocketManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketAssetRepository ticketAssetRepository;
    @Mock
    private TicketPhotoRepository ticketPhotoRepository;
    @Mock
    private TicketCommentRepository ticketCommentRepository;
    @Mock
    private com.proseg.msvc_maintenance.repository.TicketHistoryChangeRepository ticketHistoryChangeRepository;
    @Mock
    private UserCompanyRepository userCompanyRepository;
    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private TicketWebSocketManager webSocketManager;
    @Mock
    private AuthClient authClient;
    @Mock
    private MaintenanceNotificationProperties notificationProperties;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TicketServiceImpl service;

    @Test
    void create_withPhotosWithoutAttachPrivilege_throwsForbidden() {
        TicketCreateRequestDto request = TicketCreateRequestDto.builder()
                .title("Titulo")
                .description("Descripcion")
                .build();
        MultipartFile photo = mock(MultipartFile.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getAuthorities()).thenReturn(
                (Collection) List.of(new SimpleGrantedAuthority(Privileges.Tickets.CREAR)));

        assertThatThrownBy(() -> service.create(request, List.of(photo), authentication))
                .isInstanceOfSatisfying(TicketException.class, exception -> {
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getErrorCode()).isEqualTo("TICKET_ATTACH_PHOTOS_FORBIDDEN");
                });
    }
}
