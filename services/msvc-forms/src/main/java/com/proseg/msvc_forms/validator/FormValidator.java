package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;

public interface FormValidator {

    void validate(JsonNode data) throws FormValidationException;

    String getFormTypeCode();
}
