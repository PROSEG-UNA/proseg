package com.proseg.msvc_forms.controller;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.proseg.msvc_forms.dto.request.FormRecordCreateRequestDto;
import com.proseg.msvc_forms.dto.response.FormRecordResponseDto;
import com.proseg.msvc_forms.dto.response.FormTypeResponseDto;
import com.proseg.msvc_forms.security.Privileges;
import com.proseg.msvc_forms.service.FormRecordService;
import com.proseg.msvc_forms.service.FormTypeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(FormTypeAuthorizationTest.TestConfig.class)
class FormTypeAuthorizationTest {

    @Autowired
    private FormTypeController controller;

    @Autowired
    private FormTypeService formTypeService;

    @Autowired
    private FormRecordController formRecordController;

    @Autowired
    private FormRecordService formRecordService;

    @BeforeEach
    void resetMocksAndSecurityContext() {
        reset(formTypeService, formRecordService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void read_permission_can_consult_form_types() {
        UUID id = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(authentication(Privileges.Formularios.LEER));
        when(formTypeService.getAllActiveTypes()).thenReturn(List.of());
        when(formTypeService.getTypeById(id)).thenReturn(FormTypeResponseDto.builder().id(id).build());
        when(formTypeService.getTypeByCode("OVERTIME_REPORT"))
                .thenReturn(FormTypeResponseDto.builder().code("OVERTIME_REPORT").build());

        assertThatCode(controller::getActiveTypes).doesNotThrowAnyException();
        assertThatCode(() -> controller.getTypeById(id)).doesNotThrowAnyException();
        assertThatCode(() -> controller.getTypeByCode("OVERTIME_REPORT")).doesNotThrowAnyException();
    }

    @Test
    void non_read_permission_cannot_consult_form_types() {
        UUID id = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(authentication(Privileges.Formularios.CREAR));

        assertThatThrownBy(controller::getActiveTypes)
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.getTypeById(id))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.getTypeByCode("OVERTIME_REPORT"))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(formTypeService);
    }

    @Test
    void create_edit_and_delete_permissions_still_authorize_form_record_mutations() {
        UUID id = UUID.randomUUID();
        var request = new FormRecordCreateRequestDto(UUID.randomUUID(), JsonNodeFactory.instance.objectNode());

        var createAuthentication = authentication(Privileges.Formularios.CREAR);
        SecurityContextHolder.getContext().setAuthentication(createAuthentication);
        when(formRecordService.create(request, createAuthentication))
                .thenReturn(FormRecordResponseDto.builder().id(id).build());
        assertThatCode(() -> formRecordController.create(request, createAuthentication)).doesNotThrowAnyException();
        verify(formRecordService).create(eq(request), eq(createAuthentication));

        var editAuthentication = authentication(Privileges.Formularios.EDITAR);
        SecurityContextHolder.getContext().setAuthentication(editAuthentication);
        when(formRecordService.update(id, request, editAuthentication))
                .thenReturn(FormRecordResponseDto.builder().id(id).build());
        assertThatCode(() -> formRecordController.update(id, request, editAuthentication)).doesNotThrowAnyException();
        verify(formRecordService).update(eq(id), eq(request), eq(editAuthentication));

        SecurityContextHolder.getContext().setAuthentication(authentication(Privileges.Formularios.ELIMINAR));
        assertThatCode(() -> formRecordController.delete(id)).doesNotThrowAnyException();
        verify(formRecordService).delete(id);
    }

    @Test
    void wrong_permissions_cannot_create_edit_or_delete_form_records() {
        UUID id = UUID.randomUUID();
        var request = new FormRecordCreateRequestDto(UUID.randomUUID(), JsonNodeFactory.instance.objectNode());
        var authentication = authentication(Privileges.Formularios.LEER);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> formRecordController.create(request, authentication))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> formRecordController.update(id, request, authentication))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> formRecordController.delete(id))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(formRecordService);
    }

    private UsernamePasswordAuthenticationToken authentication(String authority) {
        return new UsernamePasswordAuthenticationToken(
                "user",
                "n/a",
                List.of(new SimpleGrantedAuthority(authority))
        );
    }

    @Configuration
    @EnableMethodSecurity
    @Import({FormTypeController.class, FormRecordController.class})
    static class TestConfig {

        @Bean
        FormTypeService formTypeService() {
            return mock(FormTypeService.class);
        }

        @Bean
        FormRecordService formRecordService() {
            return mock(FormRecordService.class);
        }
    }
}
