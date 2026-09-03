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
