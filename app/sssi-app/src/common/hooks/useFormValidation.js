import { useState } from 'react';
import { validateField, getValidationRule } from '../utils/validationRegex';

export function useFormValidation(initialFormData, fieldNames = []) {
  const [formData, setFormData] = useState(initialFormData);
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
  };

  const handleBlur = (e) => {
    const { name } = e.target;
    setTouched((prev) => ({ ...prev, [name]: true }));

    const rule = getValidationRule(name);
    if (rule) {
      const { isValid, error } = validateField(formData[name] || '', rule);
      setErrors((prev) => ({ ...prev, [name]: error }));
    }
  };

  const validateForm = () => {
    setTouched(prev => ({ ...prev, ...Object.fromEntries(fieldNames.map(f => [f, true])) }));
    const newErrors = {};
    let isFormValid = true;

    fieldNames.forEach((fieldName) => {
      const rule = getValidationRule(fieldName);
      if (rule) {
        const { isValid, error } = validateField(formData[fieldName] || '', rule);
        if (!isValid) {
          newErrors[fieldName] = error;
          isFormValid = false;
        }
      }
    });

    setErrors(newErrors);
    return isFormValid;
  };

  const resetForm = () => {
    setFormData(initialFormData);
    setErrors({});
    setTouched({});
  };

  const setFormValue = (name, value) => {
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  return {
    formData,
    setFormData,
    errors,
    touched,
    handleChange,
    handleBlur,
    validateForm,
    resetForm,
    setFormValue,
    isFormValid: Object.keys(errors).length === 0 && fieldNames.every((f) => touched[f]),
  };
}
