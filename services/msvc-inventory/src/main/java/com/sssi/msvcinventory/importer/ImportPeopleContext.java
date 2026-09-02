package com.sssi.msvcinventory.importer;

import com.sssi.msvcinventory.entity.Employee;
import com.sssi.msvcinventory.entity.ExecutingUnit;
import com.sssi.msvcinventory.repository.EmployeeRepository;
import com.sssi.msvcinventory.repository.ExecutingUnitRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ImportPeopleContext {

    private final EmployeeRepository employeeRepository;
    private final ExecutingUnitRepository executingUnitRepository;
    private final boolean persist;

    private Map<String, Employee> employeesByName;
    private Map<String, Employee> employeesByIdentification;
    private Map<String, ExecutingUnit> executingUnitsByName;

    public ImportPeopleContext(EmployeeRepository employeeRepository,
                               ExecutingUnitRepository executingUnitRepository,
                               boolean persist) {
        this.employeeRepository = employeeRepository;
        this.executingUnitRepository = executingUnitRepository;
        this.persist = persist;
    }

    public Optional<Employee> findEmployeeByName(String name) {
        loadEmployees();
        return Optional.ofNullable(employeesByName.get(ImportNameNormalizer.normalize(name)));
    }

    public Optional<Employee> findEmployeeByIdentification(String identification) {
        loadEmployees();
        return Optional.ofNullable(employeesByIdentification.get(ImportNameNormalizer.normalize(identification)));
    }

    public Employee createEmployee(String name, String identification) {
        loadEmployees();

        Employee employee = Employee.builder()
                .name(name)
                .identification(identification)
                .build();

        Employee stored = persist ? employeeRepository.save(employee) : employee;
        index(stored);
        return stored;
    }

    public Employee assignIdentification(Employee employee, String identification) {
        loadEmployees();

        if (persist) {
            employee.setIdentification(identification);
            Employee stored = employeeRepository.save(employee);
            index(stored);
            return stored;
        }

        Employee planned = Employee.builder()
                .id(employee.getId())
                .name(employee.getName())
                .identification(identification)
                .build();
        index(planned);
        return planned;
    }

    public ExecutingUnit findOrCreateExecutingUnit(String name) {
        loadExecutingUnits();

        String key = ImportNameNormalizer.normalize(name);
        ExecutingUnit existing = executingUnitsByName.get(key);
        if (existing != null) {
            return existing;
        }

        ExecutingUnit executingUnit = ExecutingUnit.builder()
                .name(name)
                .build();

        ExecutingUnit stored = persist ? executingUnitRepository.save(executingUnit) : executingUnit;
        executingUnitsByName.put(key, stored);
        return stored;
    }

    public boolean executingUnitExists(String name) {
        loadExecutingUnits();
        return executingUnitsByName.containsKey(ImportNameNormalizer.normalize(name));
    }

    private void index(Employee employee) {
        employeesByName.put(ImportNameNormalizer.normalize(employee.getName()), employee);
        if (employee.getIdentification() != null) {
            employeesByIdentification.put(ImportNameNormalizer.normalize(employee.getIdentification()), employee);
        }
    }

    private void loadEmployees() {
        if (employeesByName != null) {
            return;
        }
        employeesByName = new HashMap<>();
        employeesByIdentification = new HashMap<>();
        employeeRepository.findAll().forEach(this::index);
    }

    private void loadExecutingUnits() {
        if (executingUnitsByName != null) {
            return;
        }
        executingUnitsByName = new HashMap<>();
        executingUnitRepository.findAll().forEach(executingUnit ->
                executingUnitsByName.put(ImportNameNormalizer.normalize(executingUnit.getName()), executingUnit));
    }
}
