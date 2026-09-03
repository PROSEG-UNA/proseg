package com.proseg.msvc_maintenance.specification;

import com.proseg.common.specification.FilterType;
import com.proseg.common.specification.Filterable;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GenericSpecifications {

    private GenericSpecifications() {}

    private record FilterableFieldMeta(String dtoKey, FilterType type, List<String> pathSegments) {}

    private static final Map<Class<?>, Map<String, FilterableFieldMeta>> CACHE = new ConcurrentHashMap<>();

    private static Map<String, FilterableFieldMeta> getMetadata(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, GenericSpecifications::extractMetadata);
    }

    private static Map<String, FilterableFieldMeta> extractMetadata(Class<?> clazz) {
        Map<String, FilterableFieldMeta> result = new LinkedHashMap<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                Filterable annotation = field.getAnnotation(Filterable.class);
                if (annotation == null) {
                    continue;
                }
                String fieldName = field.getName();
                if (annotation.nestedPaths().length == 0) {
                    result.put(fieldName, new FilterableFieldMeta(
                            fieldName, annotation.type(), List.of(fieldName)));
                } else {
                    for (String nested : annotation.nestedPaths()) {
                        String dtoKey = fieldName + "." + nested;
                        List<String> segments = new ArrayList<>();
                        segments.add(fieldName);
                        segments.addAll(Arrays.asList(nested.split("\\.")));
                        result.put(dtoKey, new FilterableFieldMeta(
                                dtoKey, annotation.type(), Collections.unmodifiableList(segments)));
                    }
                }
            }
            current = current.getSuperclass();
        }
        return Collections.unmodifiableMap(result);
    }

    public static <T> Specification<T> withSearch(Class<T> entityClass, String term) {
        if (term == null || term.isBlank()) {
            return (root, query, cb) -> null;
        }
        Map<String, FilterableFieldMeta> metadata = getMetadata(entityClass);
        String pattern = "%" + term.strip() + "%";
        return (root, query, cb) -> {
            Map<String, Join<?, ?>> joinCache = new HashMap<>();
            List<Predicate> predicates = new ArrayList<>();
            for (FilterableFieldMeta meta : metadata.values()) {
                predicates.add(buildLikePredicate(root, cb, joinCache, meta, pattern));
            }
            if (predicates.isEmpty()) {
                return null;
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static <T> Specification<T> withColumnFilters(Class<T> entityClass, Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return (root, query, cb) -> null;
        }
        Map<String, FilterableFieldMeta> metadata = getMetadata(entityClass);
        return (root, query, cb) -> {
            Map<String, Join<?, ?>> joinCache = new HashMap<>();
            List<Predicate> predicates = new ArrayList<>();
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                FilterableFieldMeta meta = metadata.get(entry.getKey());
                if (meta == null) {
                    continue;
                }
                predicates.add(buildColumnPredicate(root, cb, joinCache, meta, entry.getValue()));
            }
            if (predicates.isEmpty()) {
                return null;
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Sort sanitizeSort(Class<?> entityClass, Sort requestedSort) {
        if (requestedSort == null || requestedSort.isUnsorted()) {
            return Sort.unsorted();
        }
        Set<String> allowed = getMetadata(entityClass).keySet();
        List<Sort.Order> valid = requestedSort.stream()
                .filter(order -> allowed.contains(order.getProperty()))
                .toList();
        return valid.isEmpty() ? Sort.unsorted() : Sort.by(valid);
    }

    private static Predicate buildLikePredicate(Root<?> root,
                                                CriteriaBuilder cb,
                                                Map<String, Join<?, ?>> joinCache,
                                                FilterableFieldMeta meta,
                                                String pattern) {
        Path<?> path = resolvePath(root, joinCache, meta.pathSegments());
        Expression<String> colExpr = normalizedExpr(cb, path);
        Expression<String> termExpr = normalizedExpr(cb, cb.literal(pattern));
        return cb.like(colExpr, termExpr);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate buildColumnPredicate(Root<?> root,
                                                  CriteriaBuilder cb,
                                                  Map<String, Join<?, ?>> joinCache,
                                                  FilterableFieldMeta meta,
                                                  String value) {
        Path<?> path = resolvePath(root, joinCache, meta.pathSegments());
        return switch (meta.type()) {
            case TEXT -> {
                Expression<String> colExpr = normalizedExpr(cb, path);
                Expression<String> termExpr = normalizedExpr(cb, cb.literal("%" + value + "%"));
                yield cb.like(colExpr, termExpr);
            }
            case ENUM -> {
                Class enumType = path.getJavaType();
                try {
                    Object enumValue = Enum.valueOf(enumType, value.toUpperCase(Locale.ROOT));
                    yield cb.equal(path, enumValue);
                } catch (IllegalArgumentException e) {
                    yield cb.disjunction();
                }
            }
            case DATE -> {
                try {
                    LocalDate date = LocalDate.parse(value);
                    yield cb.equal(path, date);
                } catch (DateTimeParseException e) {
                    yield cb.disjunction();
                }
            }
        };
    }

    private static Expression<String> normalizedExpr(CriteriaBuilder cb, Expression<?> expr) {
        return cb.lower(cb.function("unaccent", String.class, expr.as(String.class)));
    }

    private static Path<?> resolvePath(Root<?> root, Map<String, Join<?, ?>> joinCache, List<String> segments) {
        if (segments.size() == 1) {
            return root.get(segments.get(0));
        }
        String rootSegment = segments.get(0);
        Join<?, ?> join = joinCache.computeIfAbsent(rootSegment, key -> root.join(key, JoinType.LEFT));
        for (int i = 1; i < segments.size() - 1; i++) {
            String cacheKey = String.join(".", segments.subList(0, i + 1));
            final Join<?, ?> currentJoin = join;
            final String nextSegment = segments.get(i);
            join = joinCache.computeIfAbsent(cacheKey, key -> currentJoin.join(nextSegment, JoinType.LEFT));
        }
        return join.get(segments.get(segments.size() - 1));
    }
}

