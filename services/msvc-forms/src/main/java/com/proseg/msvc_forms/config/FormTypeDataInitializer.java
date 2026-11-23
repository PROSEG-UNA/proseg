package com.proseg.msvc_forms.config;

import com.proseg.msvc_forms.entity.FormType;
import com.proseg.msvc_forms.repository.FormTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FormTypeDataInitializer implements CommandLineRunner {

    private final FormTypeRepository formTypeRepository;

    @Override
    public void run(String... args) {
        seedType("OVERTIME_REPORT", "Reporte de Horas Extras",
                "Registro de horas extras del personal de seguridad.");
        seedType("ABSENCE_REPORT", "Reporte por Ausencia",
                "Registro de ausencias del personal de seguridad.");
        seedType("LATE_ARRIVAL_REPORT", "Reporte por Llegada Tardía",
                "Registro de llegadas tardías del personal de seguridad.");
        seedType("EQUIPMENT_DELIVERY", "Entrega de Equipo",
                "Registro de la entrega de equipo al personal de seguridad.");
        seedType("POST_ANOMALIES", "Anomalías en los Puestos",
                "Reporte de anomalías detectadas en los puestos.");
        seedType("SUPERVISOR_REPORT", "Reporte del/de la Supervisor/a",
                "Reporte realizado por el/la supervisor/a de turno.");
        seedType("SHIFT_CHANGE", "Cambio de Jornada",
                "Solicitud de cambio de jornada entre oficiales de seguridad.");
        seedType("VACATION_PERMIT_REQUEST", "Solicitud de Vacaciones y Otros Permisos",
                "Solicitud de vacaciones, permisos y otros trámites.");
        seedType("SERVICE_ROSTER", "Rol de Servicio",
                "Asignación de puestos y oficiales de seguridad por turno.");
        seedType("ACCESS_CONTROL", "Control de Acceso e Ingreso",
                "Registro de personas y vehículos que ingresan.");
        seedType("DAILY_ROUNDS_CONTROL", "Control de Rondas Diarias",
                "Supervisores de la Sección de Vigilancia.");
        seedType("LOGBOOK", "Bitácora",
                "Bitácora de puesto con verificación de equipo y novedades.");

        log.info("Tipos de formulario verificados");
    }

    private void seedType(String code, String name, String description) {
        FormType existing = formTypeRepository.findByCodeAndIsDeletedFalse(code).orElse(null);

        if (existing != null) {
            boolean changed = false;

            if (!name.equals(existing.getName())) {
                existing.setName(name);
                changed = true;
            }

            if (!description.equals(existing.getDescription())) {
                existing.setDescription(description);
                changed = true;
            }

            if (!existing.isActive()) {
                existing.setActive(true);
                changed = true;
            }

            if (changed) {
                formTypeRepository.save(existing);
            }
            return;
        }

        formTypeRepository.save(FormType.builder()
                .code(code)
                .name(name)
                .description(description)
                .active(true)
                .build());
    }
}
