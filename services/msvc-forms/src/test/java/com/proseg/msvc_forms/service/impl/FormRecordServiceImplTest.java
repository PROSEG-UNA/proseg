package com.proseg.msvc_forms.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvc_forms.dto.request.FormRecordCreateRequestDto;
import com.proseg.msvc_forms.dto.response.FormRecordResponseDto;
import com.proseg.msvc_forms.entity.FormRecord;
import com.proseg.msvc_forms.entity.FormType;
import com.proseg.msvc_forms.exception.FormRecordNotFoundException;
import com.proseg.msvc_forms.exception.FormTypeNotFoundException;
import com.proseg.msvc_forms.mapper.FormRecordMapper;
import com.proseg.msvc_forms.repository.FormRecordRepository;
import com.proseg.msvc_forms.repository.FormTypeRepository;
import com.proseg.msvc_forms.validator.FormValidationException;
import com.proseg.msvc_forms.validator.FormValidatorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormRecordServiceImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private FormRecordRepository formRecordRepository;
    @Mock
    private FormTypeRepository formTypeRepository;
    @Mock
    private FormRecordMapper formRecordMapper;
    @Mock
    private FormValidatorRegistry formValidatorRegistry;

    @InjectMocks
    private FormRecordServiceImpl service;

    private FormType overtimeType;
    private FormType absenceType;
    private FormType lateType;

    @BeforeEach
    void setUp() {
        overtimeType = buildType(UUID.randomUUID(), "OVERTIME_REPORT", true);
        absenceType = buildType(UUID.randomUUID(), "ABSENCE_REPORT", true);
        lateType = buildType(UUID.randomUUID(), "LATE_ARRIVAL_REPORT", true);
    }

    @Test
    void create_should_create_valid_overtime_report() {
        JsonNode payload = overtimePayloadOneRow();
        FormRecordCreateRequestDto request = new FormRecordCreateRequestDto(overtimeType.getId(), payload);
        FormRecordResponseDto response = FormRecordResponseDto.builder().id(UUID.randomUUID()).formTypeCode("OVERTIME_REPORT").data(payload).build();

        when(formTypeRepository.findById(overtimeType.getId())).thenReturn(Optional.of(overtimeType));
        when(formRecordRepository.save(any(FormRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(formRecordMapper.toResponse(any(FormRecord.class))).thenReturn(response);

        FormRecordResponseDto result = service.create(request, new TestingAuthenticationToken("user-1", null));

        assertThat(result).isSameAs(response);
        verify(formValidatorRegistry).validate("OVERTIME_REPORT", payload);
        verify(formRecordRepository).save(any(FormRecord.class));
    }

    @Test
    void create_should_create_valid_absence_report() {
        JsonNode payload = absencePayload();
        FormRecordCreateRequestDto request = new FormRecordCreateRequestDto(absenceType.getId(), payload);

        when(formTypeRepository.findById(absenceType.getId())).thenReturn(Optional.of(absenceType));
        when(formRecordRepository.save(any(FormRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(formRecordMapper.toResponse(any(FormRecord.class))).thenReturn(FormRecordResponseDto.builder().formTypeCode("ABSENCE_REPORT").build());

        FormRecordResponseDto result = service.create(request, new TestingAuthenticationToken("user-2", null));

        assertThat(result.getFormTypeCode()).isEqualTo("ABSENCE_REPORT");
        verify(formValidatorRegistry).validate("ABSENCE_REPORT", payload);
    }

    @Test
    void create_should_create_valid_late_arrival_report() {
        JsonNode payload = latePayload();
        FormRecordCreateRequestDto request = new FormRecordCreateRequestDto(lateType.getId(), payload);

        when(formTypeRepository.findById(lateType.getId())).thenReturn(Optional.of(lateType));
        when(formRecordRepository.save(any(FormRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(formRecordMapper.toResponse(any(FormRecord.class))).thenReturn(FormRecordResponseDto.builder().formTypeCode("LATE_ARRIVAL_REPORT").build());

        FormRecordResponseDto result = service.create(request, new TestingAuthenticationToken("user-3", null));

        assertThat(result.getFormTypeCode()).isEqualTo("LATE_ARRIVAL_REPORT");
        verify(formValidatorRegistry).validate("LATE_ARRIVAL_REPORT", payload);
    }

    @Test
    void create_should_fail_when_type_does_not_exist() {
        FormRecordCreateRequestDto request = new FormRecordCreateRequestDto(UUID.randomUUID(), overtimePayloadOneRow());
        when(formTypeRepository.findById(request.getFormTypeId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request, new TestingAuthenticationToken("user", null)))
                .isInstanceOf(FormTypeNotFoundException.class);
    }

    @Test
    void create_should_fail_when_type_is_inactive() {
        FormType inactiveType = buildType(UUID.randomUUID(), "OVERTIME_REPORT", false);
        FormRecordCreateRequestDto request = new FormRecordCreateRequestDto(inactiveType.getId(), overtimePayloadOneRow());
        when(formTypeRepository.findById(inactiveType.getId())).thenReturn(Optional.of(inactiveType));

        assertThatThrownBy(() -> service.create(request, new TestingAuthenticationToken("user", null)))
                .isInstanceOf(FormTypeNotFoundException.class);
    }

    @Test
    void create_should_fail_when_payload_is_incompatible_with_type() {
        JsonNode payload = latePayload();
        FormRecordCreateRequestDto request = new FormRecordCreateRequestDto(overtimeType.getId(), payload);
        when(formTypeRepository.findById(overtimeType.getId())).thenReturn(Optional.of(overtimeType));
        doThrow(new FormValidationException("INVALID_JSON", "Payload incompatible")).when(formValidatorRegistry).validate("OVERTIME_REPORT", payload);

        assertThatThrownBy(() -> service.create(request, new TestingAuthenticationToken("user", null)))
                .isInstanceOf(FormValidationException.class)
                .hasMessage("Payload incompatible");
    }

    @Test
    void create_should_fail_when_payload_is_invalid() {
        JsonNode payload = overtimePayloadEmptyDetails();
        FormRecordCreateRequestDto request = new FormRecordCreateRequestDto(overtimeType.getId(), payload);
        when(formTypeRepository.findById(overtimeType.getId())).thenReturn(Optional.of(overtimeType));
        doThrow(new FormValidationException("Debe existir al menos una fila de detalle")).when(formValidatorRegistry).validate("OVERTIME_REPORT", payload);

        assertThatThrownBy(() -> service.create(request, new TestingAuthenticationToken("user", null)))
                .isInstanceOf(FormValidationException.class);
    }

    @Test
    void getById_should_return_record() {
        UUID id = UUID.randomUUID();
        FormRecord record = FormRecord.builder().id(id).formType(overtimeType).data(overtimePayloadOneRow()).build();
        FormRecordResponseDto response = FormRecordResponseDto.builder().id(id).build();

        when(formRecordRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.of(record));
        when(formRecordMapper.toResponse(record)).thenReturn(response);

        FormRecordResponseDto result = service.getById(id);

        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    void getById_should_fail_when_id_does_not_exist() {
        UUID id = UUID.randomUUID();
        when(formRecordRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(FormRecordNotFoundException.class);
    }

    @Test
    void update_should_update_valid_record() {
        UUID id = UUID.randomUUID();
        FormRecord existing = FormRecord.builder().id(id).formType(absenceType).data(absencePayload()).build();
        JsonNode payload = overtimePayloadMultipleRows();
        FormRecordCreateRequestDto request = new FormRecordCreateRequestDto(overtimeType.getId(), payload);
        FormRecordResponseDto response = FormRecordResponseDto.builder().id(id).formTypeCode("OVERTIME_REPORT").data(payload).build();

        when(formRecordRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.of(existing));
        when(formTypeRepository.findById(overtimeType.getId())).thenReturn(Optional.of(overtimeType));
        when(formRecordRepository.save(existing)).thenReturn(existing);
        when(formRecordMapper.toResponse(existing)).thenReturn(response);

        FormRecordResponseDto result = service.update(id, request, new TestingAuthenticationToken("editor", null));

        assertThat(result.getFormTypeCode()).isEqualTo("OVERTIME_REPORT");
        assertThat(existing.getFormType()).isSameAs(overtimeType);
        assertThat(existing.getData()).isEqualTo(payload);
        verify(formValidatorRegistry).validate("OVERTIME_REPORT", payload);
    }

    @Test
    void delete_should_mark_record_as_deleted() {
        UUID id = UUID.randomUUID();
        FormRecord existing = FormRecord.builder().id(id).formType(overtimeType).data(overtimePayloadOneRow()).build();
        when(formRecordRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.of(existing));

        service.delete(id);

        assertThat(existing.isDeleted()).isTrue();
        verify(formRecordRepository).save(existing);
    }

    @Test
    void findAll_should_map_page() {
        FormRecord record = FormRecord.builder().id(UUID.randomUUID()).formType(overtimeType).data(overtimePayloadOneRow()).build();
        when(formRecordRepository.findWithFilters(null, null, PageRequest.of(0, 10))).thenReturn(new PageImpl<>(List.of(record)));
        when(formRecordMapper.toResponse(record)).thenReturn(FormRecordResponseDto.builder().id(record.getId()).build());

        assertThat(service.findAll(null, null, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }

    private FormType buildType(UUID id, String code, boolean active) {
        FormType type = FormType.builder()
                .id(id)
                .code(code)
                .name(code)
                .active(active)
                .build();
        type.markAsActive();
        return type;
    }

    private JsonNode overtimePayloadOneRow() {
        return objectMapper.valueToTree(
                java.util.Map.of(
                        "fecha", "2026-09-13",
                        "grupo", "Grupo A",
                        "supervisor", "Supervisor Uno",
                        "detalle", List.of(
                                java.util.Map.of(
                                        "no", 1,
                                        "nombre", "Juan Perez",
                                        "cedula", "1-1111-1111",
                                        "fecha", "2026-09-13",
                                        "horaEntrada", "17:00",
                                        "horaSalida", "19:30",
                                        "totalHoras", 2.5
                                )
                        ),
                        "observacion", "Observacion"
                )
        );
    }

    private JsonNode overtimePayloadMultipleRows() {
        return objectMapper.valueToTree(
                java.util.Map.of(
                        "fecha", "2026-09-13",
                        "grupo", "Grupo B",
                        "supervisor", "Supervisor Dos",
                        "detalle", List.of(
                                java.util.Map.of(
                                        "no", 1,
                                        "nombre", "Juan Perez",
                                        "cedula", "1-1111-1111",
                                        "fecha", "2026-09-13",
                                        "horaEntrada", "17:00",
                                        "horaSalida", "19:00",
                                        "totalHoras", 2.0
                                ),
                                java.util.Map.of(
                                        "no", 2,
                                        "nombre", "Maria Lopez",
                                        "cedula", "2-2222-2222",
                                        "fecha", "2026-09-13",
                                        "horaEntrada", "18:00",
                                        "horaSalida", "21:00",
                                        "totalHoras", 3.0
                                )
                        ),
                        "observacion", ""
                )
        );
    }

    private JsonNode overtimePayloadEmptyDetails() {
        return objectMapper.valueToTree(
                java.util.Map.of(
                        "fecha", "2026-09-13",
                        "grupo", "Grupo A",
                        "supervisor", "Supervisor Uno",
                        "detalle", List.of()
                )
        );
    }

    private JsonNode absencePayload() {
        return objectMapper.valueToTree(
                java.util.Map.of(
                        "fecha", "2026-09-13",
                        "hora", "07:00",
                        "guarda", "Carlos",
                        "turno", "Diurno",
                        "horaTurno", "07:00 - 15:00",
                        "puestoTrabajo", "Acceso Norte",
                        "motivoAusencia", "Incapacidad",
                        "supervisor", "Supervisor Tres"
                )
        );
    }

    private JsonNode latePayload() {
        return objectMapper.valueToTree(
                java.util.Map.of(
                        "fecha", "2026-09-13",
                        "oficialSeguridad", "Luis",
                        "operadorAcceso", "Ana",
                        "horaLlegada", "08:15",
                        "puesto", "Puerta Principal",
                        "motivo", "Transito",
                        "supervisor", "Supervisor Cuatro"
                )
        );
    }
}
