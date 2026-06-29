package com.sssi.msvcinventory.importer;

import com.sssi.msvcinventory.entity.enums.AssetStatus;

import java.text.Normalizer;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AssetStatusResolver {

    private static final Map<String, AssetStatus> VALUES_BY_NAME = Stream.of(AssetStatus.values())
            .collect(Collectors.toMap(
                    status -> normalize(status.name()),
                    Function.identity()));

    private AssetStatusResolver() {
    }

    public static Optional<AssetStatus> resolve(String rawValue) {
        String normalized = normalize(rawValue);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        if (normalized.equals("baja")) {
            return Optional.of(AssetStatus.DE_BAJA);
        }
        return Optional.ofNullable(VALUES_BY_NAME.get(normalized));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().toLowerCase().replace('_', ' ');
        String withoutAccents = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.replaceAll("\\s+", " ");
    }
}
