package com.proseg.msvc_transport.service.driverhours;

import com.proseg.msvc_transport.entity.Driver;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class RequesterMatcher {

    private static final int MIN_WORDS_TO_MATCH = 2;

    private final List<DriverWords> drivers;

    public RequesterMatcher(List<Driver> drivers) {
        this.drivers = drivers.stream().map(driver -> new DriverWords(driver, wordsOf(driver.getName()))).toList();
    }

    public Optional<Driver> match(String requester) {
        List<Driver> matches = matches(requester);
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    public List<Driver> matches(String requester) {
        Set<String> requesterWords = wordsOf(requester);
        return drivers.stream()
                .filter(candidate -> sameName(requesterWords, candidate.words()))
                .map(DriverWords::driver)
                .toList();
    }

    static Set<String> wordsOf(String name) {
        if (name == null) {
            return Set.of();
        }
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.isEmpty() ? Set.of() : new LinkedHashSet<>(Arrays.asList(normalized.split(" ")));
    }

    private static boolean sameName(Set<String> left, Set<String> right) {
        Set<String> shorter = left.size() <= right.size() ? left : right;
        Set<String> longer = shorter == left ? right : left;
        return shorter.size() >= MIN_WORDS_TO_MATCH && longer.containsAll(shorter);
    }

    private record DriverWords(Driver driver, Set<String> words) {}
}
