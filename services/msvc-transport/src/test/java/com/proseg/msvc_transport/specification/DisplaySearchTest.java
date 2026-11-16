package com.proseg.msvc_transport.specification;

import com.proseg.msvc_transport.entity.VehicleType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DisplaySearchTest {

    private Root<Object> root;
    private CriteriaBuilder criteriaBuilder;
    private Path<Object> fieldPath;
    private Predicate matchingValues;
    private Predicate noMatch;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        root = mock(Root.class);
        criteriaBuilder = mock(CriteriaBuilder.class);
        fieldPath = mock(Path.class);
        matchingValues = mock(Predicate.class);
        noMatch = mock(Predicate.class);
        when(root.get("type")).thenReturn(fieldPath);
        when(fieldPath.in(any(Collection.class))).thenReturn(matchingValues);
        when(criteriaBuilder.disjunction()).thenReturn(noMatch);
    }

    private List<Object> valuesSearchedBy(Specification<Object> specification) {
        assertThat(specification.toPredicate(root, null, criteriaBuilder)).isSameAs(matchingValues);
        ArgumentCaptor<Collection<?>> values = ArgumentCaptor.forClass(Collection.class);
        verify(fieldPath).in(values.capture());
        return List.copyOf(values.getValue());
    }

    @Test
    void matchesTheDisplayedLabelIgnoringAccentsAndCase() {
        assertThat(valuesSearchedBy(DisplaySearch.labelMatching("type", DisplayLabels.VEHICLE_TYPE, "MICROBÚS")))
                .containsExactly(VehicleType.MICROBUS);
    }

    @Test
    void matchesALabelWithSeveralWords() {
        assertThat(valuesSearchedBy(DisplaySearch.labelMatching("type", DisplayLabels.VEHICLE_TYPE, "todo terreno")))
                .containsExactly(VehicleType.ALL_TERRAIN);
    }

    @Test
    void matchesALabelWithAHyphen() {
        assertThat(valuesSearchedBy(DisplaySearch.labelMatching("type", DisplayLabels.VEHICLE_TYPE, "pick-up")))
                .containsExactly(VehicleType.PICK_UP);
    }

    @Test
    void doesNotMatchALabelWhereTheTermIsInsideAWord() {
        assertThat(valuesSearchedBy(DisplaySearch.labelMatching("type", DisplayLabels.DRIVER_STATUS, "activo")))
                .containsExactly("ACTIVE");
    }

    @Test
    void matchesALabelWhoseWordStartsWithThePartialTerm() {
        assertThat(valuesSearchedBy(DisplaySearch.labelMatching("type", DisplayLabels.DRIVER_STATUS, "inac")))
                .containsExactly("INACTIVE");
    }

    @Test
    void matchesEveryLabelThatContainsTheTerm() {
        assertThat(valuesSearchedBy(DisplaySearch.labelMatching("type", DisplayLabels.VEHICLE_AVAILABILITY, "disponible")))
                .containsExactlyInAnyOrder(true, false);
    }

    @Test
    void matchesOnlyTheLabelThatContainsTheWholeTerm() {
        assertThat(valuesSearchedBy(DisplaySearch.labelMatching("type", DisplayLabels.VEHICLE_AVAILABILITY, "no disponible")))
                .containsExactly(false);
    }

    @Test
    void matchesNothingWhenNoLabelContainsTheTerm() {
        Specification<Object> specification = DisplaySearch.labelMatching("type", DisplayLabels.VEHICLE_TYPE, "camión");

        assertThat(specification.toPredicate(root, null, criteriaBuilder)).isSameAs(noMatch);
        verify(fieldPath, never()).in(any(Collection.class));
    }

    @Test
    void addsNoConditionForABlankTerm() {
        assertThat(DisplaySearch.<Object>labelMatching("type", DisplayLabels.VEHICLE_TYPE, " ").toPredicate(root, null, criteriaBuilder)).isNull();
        assertThat(DisplaySearch.<Object>labelMatching("type", DisplayLabels.VEHICLE_TYPE, null).toPredicate(root, null, criteriaBuilder)).isNull();
        assertThat(DisplaySearch.<Object>dateMatching("departureDate", "").toPredicate(root, null, criteriaBuilder)).isNull();
        assertThat(DisplaySearch.<Object>periodMatching("year", "month", null).toPredicate(root, null, criteriaBuilder)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void matchesAMonthNameAndAYearOfThePeriod() {
        Path<Object> monthPath = mock(Path.class);
        Path<Object> yearPath = mock(Path.class);
        Expression<String> yearText = mock(Expression.class);
        when(root.get("month")).thenReturn(monthPath);
        when(root.get("year")).thenReturn(yearPath);
        when(yearPath.as(String.class)).thenReturn(yearText);

        DisplaySearch.<Object>periodMatching("year", "month", "Mayo 2026").toPredicate(root, null, criteriaBuilder);

        verify(monthPath).in(List.of(5));
        verify(criteriaBuilder).like(yearText, "%2026%");
        verify(criteriaBuilder).like(yearText, "%Mayo%");
    }

    @Test
    @SuppressWarnings("unchecked")
    void matchesAMonthNameWrittenWithoutAccentsOrInPart() {
        Path<Object> monthPath = mock(Path.class);
        Path<Object> yearPath = mock(Path.class);
        when(root.get("month")).thenReturn(monthPath);
        when(root.get("year")).thenReturn(yearPath);
        when(yearPath.as(String.class)).thenReturn(mock(Expression.class));

        DisplaySearch.<Object>periodMatching("year", "month", "set").toPredicate(root, null, criteriaBuilder);

        verify(monthPath).in(List.of(9));
    }
}
