package com.proseg.common.utils;

public final class ValidationUtils {

    public static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";

    public static final String SAFE_TEXT_REGEX = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ0-9.,()#&/\\-\\s]+$";

    public static final String COMMENT_TEXT_REGEX = "^[\\p{L}\\p{N}\\p{P}\\p{S}\\s]+$";

    public static final String PHONE_REGEX = "^[0-9+\\-\\s]{8,20}$";

    public static final String LEGAL_ID_REGEX = "^[0-9\\-]+$";

    public static final String KEYCLOAK_ID_REGEX = "^[a-zA-Z0-9\\-]+$";

    public static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}