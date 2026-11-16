package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.request.DriverRequestDto;
import com.proseg.msvc_transport.dto.response.DriverMonthResponseDto;
import com.proseg.msvc_transport.dto.response.DriverResponseDto;
import com.proseg.msvc_transport.entity.Driver;
import com.proseg.msvc_transport.entity.DriverUnit;
import com.proseg.msvc_transport.exception.TransportException;
import com.proseg.msvc_transport.repository.DriverRepository;
import com.proseg.msvc_transport.service.driverhours.DriverHoursRules;
import com.proseg.msvc_transport.service.driverhours.OrdinaryWindow;
import com.proseg.msvc_transport.service.driverhours.UlloaTours;
import com.proseg.msvc_transport.specification.DisplayLabels;
import com.proseg.msvc_transport.specification.DisplaySearch;
import com.proseg.msvc_transport.specification.GenericSpecifications;
import jakarta.persistence.criteria.Expression;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverService {

    private static final Map<String, Function<DriverMonthResponseDto, Double>> MONTH_SORT_VALUES = Map.of(
            "businessDays", month -> asDouble(month.getBusinessDays()),
            "ordinaryHoursAccrued", month -> asDouble(month.getOrdinaryHoursAccrued()),
            "surplusExecutionRatio", DriverService::surplusExecutionRatio,
            "surplusHoursAccrued", month -> asDouble(month.getSurplusHoursAccrued()),
            "overtimeHoursAccrued", month -> asDouble(month.getOvertimeHoursAccrued()),
            "weekendsWorked", month -> asDouble(month.getWeekendsWorked())
    );

    public static final Set<String> MONTH_SORT_FIELDS = MONTH_SORT_VALUES.keySet();

    private static final String SCHEDULE_TIME_FORMAT = "HH24:MI";

    private final DriverRepository driverRepository;
    private final DraftCatalogService draftCatalogService;

    @Transactional
    public DriverResponseDto create(DriverRequestDto request) {
        if (driverRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw TransportException.conflict("DRIVER_DUPLICATE_NAME", "Ya existe un chofer con ese nombre");
        }
        Driver driver = toEntity(request);
        Driver saved = driverRepository.save(driver);
        draftCatalogService.syncDriver(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DriverResponseDto findById(UUID id) {
        return driverRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> TransportException.notFound("Chofer", id.toString()));
    }

    @Transactional(readOnly = true)
    public Page<DriverResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable) {
        return findAll(search, filters, pageable, Map.of());
    }

    @Transactional(readOnly = true)
    public Page<DriverResponseDto> findAll(String search, Map<String, String> filters, Pageable pageable, Map<UUID, DriverMonthResponseDto> monthsByDriver) {
        Specification<Driver> spec = specificationOf(search, filters);
        if (!monthsByDriver.isEmpty() && pageable.getSort().stream().anyMatch(order -> MONTH_SORT_VALUES.containsKey(order.getProperty()))) {
            return findAllSortedByMonth(spec, pageable, monthsByDriver);
        }
        Pageable sanitized = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), GenericSpecifications.sanitizeSort(Driver.class, pageable.getSort()));
        return driverRepository.findAll(spec, sanitized).map(this::toResponse);
    }

    private Page<DriverResponseDto> findAllSortedByMonth(Specification<Driver> spec, Pageable pageable, Map<UUID, DriverMonthResponseDto> monthsByDriver) {
        Comparator<Driver> byMonthColumns = pageable.getSort().stream()
                .filter(order -> MONTH_SORT_VALUES.containsKey(order.getProperty()))
                .map(order -> monthColumnComparator(order, monthsByDriver))
                .reduce(Comparator::thenComparing)
                .orElseThrow();
        List<Driver> sorted = driverRepository.findAll(spec).stream()
                .sorted(byMonthColumns
                        .thenComparing(Driver::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Driver::getId))
                .toList();
        int from = (int) Math.min(pageable.getOffset(), sorted.size());
        int to = Math.min(from + pageable.getPageSize(), sorted.size());
        List<DriverResponseDto> pageContent = sorted.subList(from, to).stream().map(this::toResponse).toList();
        return new PageImpl<>(pageContent, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()), sorted.size());
    }

    private static Comparator<Driver> monthColumnComparator(Sort.Order order, Map<UUID, DriverMonthResponseDto> monthsByDriver) {
        Function<DriverMonthResponseDto, Double> valueOf = MONTH_SORT_VALUES.get(order.getProperty());
        Comparator<Double> byValue = order.isAscending() ? Comparator.naturalOrder() : Comparator.reverseOrder();
        return Comparator.comparing(
                driver -> Optional.ofNullable(monthsByDriver.get(driver.getId())).map(valueOf).orElse(null),
                Comparator.nullsLast(byValue));
    }

    static Specification<Driver> ordinaryScheduleMatching(String term) {
        if (term == null || term.isBlank()) {
            return (root, query, cb) -> null;
        }
        String pattern = "%" + term.strip() + "%";
        return (root, query, cb) -> {
            Expression<String> start = cb.function("to_char", String.class, root.get("ordinaryStart"), cb.literal(SCHEDULE_TIME_FORMAT));
            Expression<String> end = cb.function("to_char", String.class, root.get("ordinaryEnd"), cb.literal(SCHEDULE_TIME_FORMAT));
            Expression<String> storedSchedule = cb.concat(cb.concat(start, " a "), end);
            Expression<String> displayedSchedule = cb.<String>selectCase()
                    .when(cb.or(cb.isNull(root.get("ordinaryStart")), cb.isNull(root.get("ordinaryEnd"))), OrdinaryWindow.STANDARD.label())
                    .otherwise(storedSchedule);
            return cb.like(displayedSchedule, pattern);
        };
    }

    private static Double surplusExecutionRatio(DriverMonthResponseDto month) {
        Double cap = month.getSurplusCap();
        if (cap == null || cap <= 0 || month.getSurplusHoursAccrued() == null) return null;
        return month.getSurplusHoursAccrued() / cap;
    }

    private static Double asDouble(Number value) {
        return value == null ? null : value.doubleValue();
    }

    private Specification<Driver> specificationOf(String search, Map<String, String> filters) {
        Map<String, String> columnFilters = new HashMap<>(filters);
        String status = columnFilters.remove("status");
        String ids = columnFilters.remove("ids");
        Specification<Driver> spec = Specification
                .where(GenericSpecifications.withSearch(
                        Driver.class,
                        search,
                        DisplaySearch.labelMatching("status", DisplayLabels.DRIVER_STATUS, search),
                        ordinaryScheduleMatching(search)))
                .and(GenericSpecifications.withColumnFilters(Driver.class, columnFilters));
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("status")), status.toUpperCase(Locale.ROOT)));
        }
        if (ids != null) {
            Set<UUID> driverIds = parseIds(ids);
            spec = spec.and((root, query, cb) -> driverIds.isEmpty() ? cb.disjunction() : root.get("id").in(driverIds));
        }
        return spec;
    }

    private Set<UUID> parseIds(String ids) {
        try {
            return Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(UUID::fromString)
                    .collect(Collectors.toSet());
        } catch (IllegalArgumentException exception) {
            throw TransportException.badRequest("INVALID_DRIVER_IDS", "La lista de choferes no es válida");
        }
    }

    @Transactional
    public DriverResponseDto update(UUID id, DriverRequestDto request) {
        Driver driver = driverRepository.findById(id).orElseThrow(() -> TransportException.notFound("Chofer", id.toString()));
        if (driverRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
            throw TransportException.conflict("DRIVER_DUPLICATE_NAME", "Ya existe un chofer con ese nombre");
        }
        updateEntity(driver, request);
        Driver saved = driverRepository.save(driver);
        draftCatalogService.syncDriver(saved);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Driver driver = driverRepository.findById(id).orElseThrow(() -> TransportException.notFound("Chofer", id.toString()));
        draftCatalogService.removeDriver(id);
        driverRepository.delete(driver);
    }

    private Driver toEntity(DriverRequestDto request) {
        Driver driver = new Driver();
        updateEntity(driver, request);
        return driver;
    }

    private void updateEntity(Driver entity, DriverRequestDto request) {
        OrdinaryWindow window = ordinaryWindowOf(request);
        entity.setName(request.getName().trim());
        entity.setStatus(request.getStatus());
        entity.setOrganizationalUnit(request.getOrganizationalUnit() == null
                ? Optional.ofNullable(entity.getOrganizationalUnit()).orElse(DriverUnit.GENERAL_SERVICES)
                : request.getOrganizationalUnit());
        entity.setRestrictions(request.getRestrictions());
        entity.setAllowedVehicleTypes(new HashSet<>(request.getAllowedVehicleTypes()));
        entity.setOrdinaryStart(window.startTime());
        entity.setOrdinaryEnd(window.endTime());
        entity.setUlloaShifts(request.getUlloaShifts().isEmpty()
                ? new HashSet<>()
                : new HashSet<>(UlloaTours.shiftsCoveredBy(window)));
    }

    private OrdinaryWindow ordinaryWindowOf(DriverRequestDto request) {
        LocalTime start = Optional.ofNullable(request.getOrdinaryStart()).orElseGet(OrdinaryWindow.STANDARD::startTime);
        LocalTime end = Optional.ofNullable(request.getOrdinaryEnd()).orElseGet(OrdinaryWindow.STANDARD::endTime);
        return OrdinaryWindow.allowed(start, end).orElseThrow(() -> TransportException.unprocessable(
                "DRIVER_INVALID_ORDINARY_SCHEDULE",
                "El horario ordinario debe ser de " + OrdinaryWindow.allowedDescription()
        ));
    }

    public DriverResponseDto toResponse(Driver entity) {
        OrdinaryWindow window = OrdinaryWindow.of(entity);
        return DriverResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .status(entity.getStatus())
                .organizationalUnit(entity.getOrganizationalUnit())
                .restrictions(entity.getRestrictions())
                .allowedVehicleTypes(entity.getAllowedVehicleTypes().stream().sorted().toList())
                .ordinaryStart(DriverHoursRules.timeOf(window.start()))
                .ordinaryEnd(DriverHoursRules.timeOf(window.end()))
                .ulloaShifts(entity.getUlloaShifts().stream().sorted().toList())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
