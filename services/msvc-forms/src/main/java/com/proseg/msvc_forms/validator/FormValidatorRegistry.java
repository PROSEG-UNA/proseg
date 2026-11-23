package com.proseg.msvc_forms.validator;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormValidatorRegistry {

    private final List<FormValidator> validators;
    private Map<String, FormValidator> validatorMap;

    private Map<String, FormValidator> getValidators() {
        if (validatorMap == null) {
            validatorMap = validators.stream()
                .collect(Collectors.toMap(FormValidator::getFormTypeCode, Function.identity()));
        }
        return validatorMap;
    }

    public void validate(String formTypeCode, JsonNode data) throws FormValidationException {
        FormValidator validator = getValidators().get(formTypeCode);
        
        if (validator == null) {
            throw new FormValidationException("UNKNOWN_FORM_TYPE", 
                "No existe validador para el tipo de formulario: " + formTypeCode);
        }
        
        validator.validate(data);
    }

    public boolean hasValidator(String formTypeCode) {
        return getValidators().containsKey(formTypeCode);
    }
}
