package com.proseg.msvc_transport.service;

import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftAssignmentDto;
import com.proseg.msvc_transport.dto.tourprocessing.TourProcessingDraftRowResponseDto;
import com.proseg.msvc_transport.entity.*;
import com.proseg.msvc_transport.repository.*;
import com.proseg.msvc_transport.specification.GenericSpecifications;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DraftTourStore {
    private final TourProcessingDraftTourRepository tours;
    private final TourProcessingDraftImportRowRepository imports;
    private final TourProcessingDraftRowIndexRepository index;
    private final EntityManager entityManager;

    public List<TourProcessingDraftTour> findAllByDraftIdOrderByRowNumberAsc(UUID draftId) {
        List<TourProcessingDraftTour> result = new ArrayList<>(tours.findAllByDraftIdOrderByRowNumberAsc(draftId));
        imports.findAllByDraftId(draftId).stream().map(this::restore).forEach(result::add);
        result.sort(Comparator.comparing(TourProcessingDraftTour::getRowNumber).thenComparing(TourProcessingDraftTour::getId));
        return result;
    }

    public List<TourProcessingDraftTour> findPrimariesByDraftId(UUID draftId) {
        return tours.findAllByDraftIdOrderByRowNumberAsc(draftId);
    }

    public Optional<TourProcessingDraftTour> findByIdAndDraftId(UUID id, UUID draftId) {
        return tours.findByIdAndDraftId(id, draftId).or(() -> imports.findById(id)
                .filter(row -> draftId.equals(row.getDraftId())).map(this::restore));
    }

    public List<TourProcessingDraftTour> findAllByIdIn(Collection<UUID> ids) {
        return tours.findAllByIdIn(ids);
    }

    public List<TourProcessingDraftTour> findPrimariesByNumbers(UUID draftId, Collection<String> numbers) {
        return tours.findPrimariesByNumbers(draftId, numbers);
    }

    public Page<TourProcessingDraftTour> findPage(UUID draftId, String search, Map<String, String> filters, Pageable pageable) {
        return findPage(draftId, search, filters, null, pageable);
    }

    public Page<TourProcessingDraftTour> findPage(
            UUID draftId,
            String search,
            Map<String, String> filters,
            Collection<UUID> tourIds,
            Pageable pageable
    ) {
        if (tourIds != null && tourIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        Specification<TourProcessingDraftRowIndex> spec = (root, query, cb) -> cb.equal(root.get("draftId"), draftId);
        if (tourIds != null) {
            spec = spec.and((root, query, cb) -> root.get("id").in(tourIds));
        }
        spec = spec.and(GenericSpecifications.withSearch(TourProcessingDraftRowIndex.class, search))
                .and(GenericSpecifications.withColumnFilters(TourProcessingDraftRowIndex.class, filters));
        Sort sort = GenericSpecifications.sanitizeSort(TourProcessingDraftRowIndex.class, pageable.getSort());
        Page<TourProcessingDraftRowIndex> page = index.findAll(spec, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                sort.isUnsorted() ? Sort.by("rowNumber").and(Sort.by("id")) : sort.and(Sort.by("id"))));
        List<UUID> ids = page.map(TourProcessingDraftRowIndex::getId).getContent();
        if (ids.isEmpty()) return new PageImpl<>(List.of(), pageable, page.getTotalElements());
        Map<UUID, TourProcessingDraftTour> byId = tours.findAllByIdIn(ids).stream()
                .collect(Collectors.toMap(TourProcessingDraftTour::getId, tour -> tour));
        imports.findAllById(ids).forEach(row -> byId.put(row.getId(), restore(row)));
        return page.map(row -> byId.get(row.getId()));
    }

    public TourProcessingDraftTour save(TourProcessingDraftTour tour) {
        saveAll(List.of(tour));
        return tour;
    }

    public void saveAll(List<TourProcessingDraftTour> rows) {
        if (rows.isEmpty()) return;
        List<UUID> ids = rows.stream().map(TourProcessingDraftTour::getId).filter(Objects::nonNull).toList();
        Map<UUID, TourProcessingDraftTour> existing = tours.findAllByIdIn(ids).stream()
                .collect(Collectors.toMap(TourProcessingDraftTour::getId, tour -> tour));
        Map<UUID, TourProcessingDraftImportRow> raw = imports.findAllById(ids).stream()
                .collect(Collectors.toMap(TourProcessingDraftImportRow::getId, row -> row));
        for (TourProcessingDraftTour row : rows) {
            if (row.getId() == null) row.setId(UUID.randomUUID());
            if (row.isPrimary()) {
                if (raw.containsKey(row.getId())) imports.delete(raw.get(row.getId()));
                if (!existing.containsKey(row.getId())) entityManager.persist(row);
            } else {
                TourProcessingDraftImportRow stored = raw.getOrDefault(row.getId(), TourProcessingDraftImportRow.builder()
                        .id(row.getId()).draftId(row.getDraft().getId()).build());
                var payload = snapshot(row);
                if (!Objects.equals(stored.getPayload(), payload)) {
                    stored.setPayload(payload);
                    imports.save(stored);
                }
                if (existing.containsKey(row.getId())) tours.delete(existing.get(row.getId()));
            }
        }
        entityManager.flush();
    }

    public void delete(TourProcessingDraftTour tour) {
        if (tour.isPrimary()) tours.delete(tour);
        else imports.deleteById(tour.getId());
    }

    public void deleteAll(List<TourProcessingDraftTour> rows) {
        rows.forEach(this::delete);
    }

    public void flush() {
        entityManager.flush();
    }

    public static TourProcessingDraftRowResponseDto snapshot(TourProcessingDraftTour tour) {
        TourProcessingDraftRowResponseDto payload = new TourProcessingDraftRowResponseDto();
        BeanUtils.copyProperties(tour, payload, "assignments", "driver", "vehicle");
        payload.setAssignments(tour.getAssignments().stream().map(part -> {
            TourProcessingDraftAssignmentDto assignment = new TourProcessingDraftAssignmentDto();
            BeanUtils.copyProperties(part, assignment);
            assignment.setRowId(part.getId());
            assignment.setBorrowedVehicleId(part.getBorrowedVehicle() == null ? null : part.getBorrowedVehicle().getId());
            return assignment;
        }).toList());
        if (tour.primaryAssignment() != null) {
            payload.setDriver(tour.primaryAssignment().getDriver());
            payload.setVehicle(tour.primaryAssignment().getVehicle());
        }
        return payload;
    }

    private TourProcessingDraftTour restore(TourProcessingDraftImportRow row) {
        TourProcessingDraftTour tour = new TourProcessingDraftTour();
        BeanUtils.copyProperties(row.getPayload(), tour, "assignments", "driver", "vehicle");
        tour.setId(row.getId());
        tour.setDraft(entityManager.getReference(TourProcessingDraft.class, row.getDraftId()));
        for (TourProcessingDraftAssignmentDto part : row.getPayload().getAssignments()) {
            TourProcessingDraftAssignment assignment = new TourProcessingDraftAssignment();
            BeanUtils.copyProperties(part, assignment);
            assignment.setId(part.getRowId());
            if (part.getBorrowedVehicleId() != null) {
                assignment.setBorrowedVehicle(entityManager.getReference(BorrowedVehicle.class, part.getBorrowedVehicleId()));
            }
            tour.addAssignment(assignment);
        }
        return tour;
    }
}
