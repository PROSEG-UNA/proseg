package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

public abstract class SpecFormValidator implements FormValidator {

    private static final Pattern PHONE = Pattern.compile("^[0-9+()\\-\\s]{7,20}$");
    private static final int MAX_TEXT_LENGTH = 5000;

    protected abstract List<FieldSpec> specs();

    protected void validateExtra(JsonNode data) {
    }

    @Override
    public void validate(JsonNode data) throws FormValidationException {
        if (data == null || !data.isObject()) {
            throw new FormValidationException("INVALID_JSON", "Los datos del formulario no son válidos");
        }
        validateObject(data, specs(), "");
        validateExtra(data);
    }

    protected static boolean isBlank(JsonNode node) {
        return node == null || node.isNull() || (node.isTextual() && node.asText().isBlank());
    }

    protected static String textOf(JsonNode data, String key) {
        JsonNode node = data.get(key);
        return isBlank(node) ? null : node.asText();
    }

    private void validateObject(JsonNode obj, List<FieldSpec> specs, String prefix) {
        for (FieldSpec spec : specs) {
            validateField(obj.get(spec.key()), spec, prefix);
        }
    }

    private void validateField(JsonNode value, FieldSpec spec, String prefix) {
        String name = prefix + spec.label();

        if (isBlank(value)) {
            if (spec.mandatory()) {
                throw new FormValidationException(name + " es obligatorio");
            }
            return;
        }

        switch (spec.type()) {
            case TEXT -> {
                if (!value.isTextual() || value.asText().length() > MAX_TEXT_LENGTH) {
                    throw new FormValidationException(name + " no es válido");
                }
            }
            case DATE -> {
                try {
                    LocalDate.parse(value.asText());
                } catch (DateTimeParseException ex) {
                    throw new FormValidationException(name + " no es una fecha válida");
                }
            }
            case TIME -> {
                try {
                    LocalTime.parse(value.asText());
                } catch (DateTimeParseException ex) {
                    throw new FormValidationException(name + " no es una hora válida");
                }
            }
            case INT -> {
                if (!value.isIntegralNumber() || value.asInt() < spec.min()) {
                    throw new FormValidationException(name + " debe ser un número entero mayor o igual a " + spec.min());
                }
            }
            case BOOL -> {
                if (!value.isBoolean()) {
                    throw new FormValidationException(name + " no es válido");
                }
            }
            case ENUM -> {
                if (!value.isTextual() || !spec.options().contains(value.asText())) {
                    throw new FormValidationException(name + " debe ser uno de: " + String.join(", ", spec.options()));
                }
            }
            case PHONE -> {
                if (!value.isTextual() || !PHONE.matcher(value.asText().trim()).matches()) {
                    throw new FormValidationException(name + " no es un teléfono válido");
                }
            }
            case OBJECT -> {
                if (!value.isObject()) {
                    throw new FormValidationException(name + " no es válido");
                }
                validateObject(value, spec.children(), name + ": ");
            }
            case LIST -> {
                if (!value.isArray()) {
                    throw new FormValidationException(name + " no es válido");
                }
                if (value.size() < spec.min()) {
                    throw new FormValidationException("Debe existir al menos " + spec.min() + " fila en " + spec.label());
                }
                for (int i = 0; i < value.size(); i++) {
                    JsonNode row = value.get(i);
                    if (!row.isObject()) {
                        throw new FormValidationException("Fila " + (i + 1) + " de " + spec.label() + " no es válida");
                    }
                    validateObject(row, spec.children(), "Fila " + (i + 1) + ": ");
                }
            }
        }
    }
}
