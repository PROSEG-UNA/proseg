package com.proseg.msvcinventory.importer;

import com.proseg.msvcinventory.entity.Employee;
import com.proseg.msvcinventory.entity.ExecutingUnit;
import com.proseg.msvcinventory.exception.AssetImportException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
public class ResponsibleResolver {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_IDENTIFICATION_LENGTH = 100;

    public ResponsibleResolution resolveEmployee(String rawName, String rawIdentification, ImportPeopleContext context) {
        String name = trimToNull(rawName);
        String identification = trimToNull(rawIdentification);

        if (name == null && identification == null) {
            return new ResponsibleResolution.None();
        }

        if (name != null && name.length() > MAX_NAME_LENGTH) {
            throw AssetImportException.fieldRule(
                    "El nombre del funcionario no puede superar los " + MAX_NAME_LENGTH + " caracteres");
        }
        if (identification != null && identification.length() > MAX_IDENTIFICATION_LENGTH) {
            throw AssetImportException.fieldRule(
                    "La identificación del funcionario no puede superar los " + MAX_IDENTIFICATION_LENGTH + " caracteres");
        }

        Optional<Employee> byName = name == null
                ? Optional.empty()
                : context.findEmployeeByName(name);
        Optional<Employee> byIdentification = identification == null
                ? Optional.empty()
                : context.findEmployeeByIdentification(identification);

        if (identification == null) {
            return byName.<ResponsibleResolution>map(ResponsibleResolution.Use::new)
                    .orElseGet(() -> new ResponsibleResolution.Create(name, null));
        }

        if (name == null) {
            return byIdentification.<ResponsibleResolution>map(ResponsibleResolution.Use::new)
                    .orElseThrow(() -> AssetImportException.employeeIdentificationNotFound(rawIdentification.trim()));
        }

        if (byName.isPresent() && byIdentification.isPresent()) {
            if (isSameEmployee(byName.get(), byIdentification.get())) {
                return new ResponsibleResolution.Use(byName.get());
            }
            throw AssetImportException.employeeConflict(
                    name, identification,
                    byName.get().getIdentification(), byIdentification.get().getName());
        }

        if (byName.isPresent()) {
            Employee employee = byName.get();
            if (employee.getIdentification() == null) {
                return new ResponsibleResolution.AssignIdentification(employee, identification);
            }
            throw AssetImportException.employeeIdentificationMismatch(
                    name, employee.getIdentification(), identification);
        }

        if (byIdentification.isPresent()) {
            throw AssetImportException.employeeNameMismatch(
                    identification, byIdentification.get().getName(), name);
        }

        return new ResponsibleResolution.Create(name, identification);
    }

    public Employee apply(ResponsibleResolution resolution, ImportPeopleContext context) {
        if (resolution instanceof ResponsibleResolution.Use use) {
            return use.employee();
        }
        if (resolution instanceof ResponsibleResolution.Create create) {
            return context.createEmployee(create.name(), create.identification());
        }
        if (resolution instanceof ResponsibleResolution.AssignIdentification assign) {
            return context.assignIdentification(assign.employee(), assign.identification());
        }
        return null;
    }

    public ExecutingUnit resolveExecutingUnit(String rawName, ImportPeopleContext context) {
        String name = trimToNull(rawName);
        if (name == null) {
            return null;
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw AssetImportException.fieldRule(
                    "La unidad ejecutora no puede superar los " + MAX_NAME_LENGTH + " caracteres");
        }
        return context.findOrCreateExecutingUnit(name);
    }

    private boolean isSameEmployee(Employee first, Employee second) {
        if (first.getId() != null && second.getId() != null) {
            return Objects.equals(first.getId(), second.getId());
        }
        return first == second;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
