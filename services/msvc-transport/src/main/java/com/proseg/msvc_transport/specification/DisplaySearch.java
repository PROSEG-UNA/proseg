package com.proseg.msvc_transport.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public final class DisplaySearch {

    private static final String DISPLAY_DATE_FORMAT = "DD/MM/YYYY";

    private DisplaySearch() {}

    public static <T> Specification<T> labelMatching(String field, Map<?, String> labelsByValue, String term) {
        if (term == null || term.isBlank()) {
            return (root, query, cb) -> null;
        }
        String wanted = normalized(term);
        List<Object> matchingValues = labelsByValue.entrySet().stream()
                .filter(entry -> startsAWord(normalized(entry.getValue()), wanted))
                .map(entry -> (Object) entry.getKey())
                .toList();
        return (root, query, cb) -> matchingValues.isEmpty() ? cb.disjunction() : root.get(field).in(matchingValues);
    }

    public static <T> Specification<T> dateMatching(String field, String term) {
        if (term == null || term.isBlank()) {
            return (root, query, cb) -> null;
        }
        String pattern = "%" + term.strip() + "%";
        return (root, query, cb) -> cb.like(
                cb.function("to_char", String.class, root.get(field), cb.literal(DISPLAY_DATE_FORMAT)),
                pattern);
    }

    public static <T> Specification<T> periodMatching(String yearField, String monthField, String term) {
        if (term == null || term.isBlank()) {
            return (root, query, cb) -> null;
        }
        List<String> tokens = Arrays.asList(term.strip().split("\\s+"));
        return (root, query, cb) -> cb.and(tokens.stream()
                .map(token -> {
                    List<Integer> months = monthNumbersMatching(token);
                    Predicate monthMatches = months.isEmpty() ? cb.disjunction() : root.get(monthField).in(months);
                    Predicate yearMatches = cb.like(root.get(yearField).as(String.class), "%" + token + "%");
                    return cb.or(monthMatches, yearMatches);
                })
                .toArray(Predicate[]::new));
    }

    private static List<Integer> monthNumbersMatching(String token) {
        String wanted = normalized(token);
        return IntStream.rangeClosed(1, DisplayLabels.MONTH_NAMES.size())
                .filter(number -> startsAWord(normalized(DisplayLabels.MONTH_NAMES.get(number - 1)), wanted))
                .boxed()
                .toList();
    }

    private static boolean startsAWord(String normalizedLabel, String normalizedTerm) {
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(normalizedTerm)).matcher(normalizedLabel).find();
    }

    static String normalized(String text) {
        return Normalizer.normalize(text.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
