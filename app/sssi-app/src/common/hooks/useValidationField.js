/**
 * useValidationField Hook
 * Maneja la validación y estado de error para un campo individual
 * 
 * Uso en un componente:
 * const { value, error, touched, handleChange, handleBlur } = useValidationField('email');
 * 
 * <TextField
 *   value={value}
 *   onChange={handleChange}
 *   onBlur={handleBlur}
 *   error={touched && !!error}
 *   helperText={touched && error}
 * />
 */

import { useState } from 'react';
import { validateField, getValidationRule } from '../utils/validationRegex';

export function useValidationField(fieldName, initialValue = '') {
  const [value, setValue] = useState(initialValue);
  const [touched, setTouched] = useState(false);
  const [error, setError] = useState('');

  const rule = getValidationRule(fieldName);

  const handleChange = (e) => {
    const newValue = e.target.value;
    setValue(newValue);

    // Validar en tiempo real si el campo ya fue tocado
    if (touched && rule) {
      const { isValid, error: validationError } = validateField(newValue, rule);
      setError(validationError);
    }
  };

  const handleBlur = () => {
    setTouched(true);
    if (rule) {
      const { isValid, error: validationError } = validateField(value, rule);
      setError(validationError);
    }
  };

  const validate = () => {
    if (rule) {
      const { isValid, error: validationError } = validateField(value, rule);
      if (!isValid) setError(validationError);
      return isValid;
    }
    return true;
  };

  const reset = () => {
    setValue(initialValue);
    setTouched(false);
    setError('');
  };

  return {
    value,
    setValue,
    error,
    touched,
    setTouched,
    handleChange,
    handleBlur,
    validate,
    reset,
    isValid: !error,
  };
}
