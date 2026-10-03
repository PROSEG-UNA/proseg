package com.proseg.msvc_forms.validator;

import java.util.List;

public record FieldSpec(String key, String label, Type type, boolean mandatory,
                        List<String> options, int min, List<FieldSpec> children) {

    public enum Type { TEXT, DATE, TIME, INT, BOOL, ENUM, PHONE, OBJECT, LIST }

    public static final List<String> YES_NO = List.of("SI", "NO");

    public static FieldSpec text(String key, String label) {
        return new FieldSpec(key, label, Type.TEXT, false, List.of(), 0, List.of());
    }

    public static FieldSpec date(String key, String label) {
        return new FieldSpec(key, label, Type.DATE, false, List.of(), 0, List.of());
    }

    public static FieldSpec time(String key, String label) {
        return new FieldSpec(key, label, Type.TIME, false, List.of(), 0, List.of());
    }

    public static FieldSpec integer(String key, String label, int min) {
        return new FieldSpec(key, label, Type.INT, false, List.of(), min, List.of());
    }

    public static FieldSpec phone(String key, String label) {
        return new FieldSpec(key, label, Type.PHONE, false, List.of(), 0, List.of());
    }

    public static FieldSpec choice(String key, String label, List<String> options) {
        return new FieldSpec(key, label, Type.ENUM, false, options, 0, List.of());
    }

    public static FieldSpec object(String key, String label, List<FieldSpec> children) {
        return new FieldSpec(key, label, Type.OBJECT, false, List.of(), 0, children);
    }

    public static FieldSpec list(String key, String label, int minRows, List<FieldSpec> children) {
        return new FieldSpec(key, label, Type.LIST, true, List.of(), minRows, children);
    }

    // Elemento con casilla "entregado" y descripcion opcional
    public static FieldSpec checkItem(String key, String label) {
        return object(key, label, List.of(
                new FieldSpec("entregado", "Entregado", Type.BOOL, false, List.of(), 0, List.of()),
                text("descripcion", "Descripción")));
    }

    // Elemento con verificacion Si/No, serie y cantidad opcionales
    public static FieldSpec yesNoItem(String key, String label) {
        return object(key, label, List.of(
                choice("valor", "Valor", YES_NO),
                text("serie", "Serie"),
                integer("cantidad", "Cantidad", 0)));
    }

    public FieldSpec required() {
        return new FieldSpec(key, label, type, true, options, min, children);
    }
}
