package com.sssi.msvcinventory.service.impl;

import com.sssi.msvcinventory.dto.request.EmployeeRequestDto;
import com.sssi.msvcinventory.dto.response.EmployeeResponseDto;
import com.sssi.msvcinventory.entity.Employee;
import com.sssi.msvcinventory.exception.EmployeeException;
import com.sssi.msvcinventory.mapper.EmployeeMapper;
import com.sssi.msvcinventory.repository.AssetRepository;
import com.sssi.msvcinventory.repository.EmployeeRepository;
import com.sssi.msvcinventory.service.EmployeeService;
import com.sssi.msvcinventory.specification.GenericSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AssetRepository assetRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    @Transactional
    public EmployeeResponseDto create(EmployeeRequestDto request) {

        request.setIdentification(trimToNull(request.getIdentification()));

        if (employeeRepository.existsByNameIgnoreCase(request.getName())) {
            throw EmployeeException.duplicateName(request.getName());
        }

        if (request.getIdentification() != null
                && employeeRepository.existsByIdentificationIgnoreCase(request.getIdentification())) {
            throw EmployeeException.duplicateIdentification(request.getIdentification());
        }

        Employee employee = employeeMapper.toEntity(request);
        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDto findById(UUID id) {
        return employeeRepository.findById(id)
                .map(employeeMapper::toResponse)
                .orElseThrow(() -> EmployeeException.notFound(id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        Specification<Employee> spec = Specification
                .where(GenericSpecifications.<Employee>withSearch(Employee.class, search))
                .and(GenericSpecifications.<Employee>withColumnFilters(Employee.class, filters));

        Pageable sanitized = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                GenericSpecifications.sanitizeSort(Employee.class, pageable.getSort())
        );

        return employeeRepository.findAll(spec, sanitized).map(employeeMapper::toResponse);
    }

    @Override
    @Transactional
    public EmployeeResponseDto update(UUID id, EmployeeRequestDto request) {

        request.setIdentification(trimToNull(request.getIdentification()));

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> EmployeeException.notFound(id.toString()));

        if (employeeRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw EmployeeException.duplicateName(request.getName());
        }

        if (request.getIdentification() != null
                && employeeRepository.existsByIdentificationIgnoreCaseAndIdNot(request.getIdentification(), id)) {
            throw EmployeeException.duplicateIdentification(request.getIdentification());
        }

        employeeMapper.updateEntityFromRequest(request, employee);
        employee.setIdentification(request.getIdentification());

        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> EmployeeException.notFound(id.toString()));

        if (assetRepository.existsByEmployeeId(id)) {
            throw EmployeeException.inUse(employee.getName());
        }

        employeeRepository.delete(employee);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
