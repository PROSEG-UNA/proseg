/**
 * ValidatedTextField Component
 * Envuelve TextField de Material-UI con validación automática
 * 
 * Uso:
 * <ValidatedTextField
 *   fieldName="email"
 *   value={formData.email}
 *   onChange={handleChange}
 *   onBlur={handleBlur}
 *   error={!!errors.email}
 *   helperText={errors.email}
 *   touched={touched.email}
 * />
 */

import { TextField } from '@mui/material';
import { getValidationRule } from '../utils/validationRegex';

export function ValidatedTextField({
  fieldName,
  value = '',
  onChange,
  onBlur,
  error = false,
  helperText = '',
  touched = false,
  disabled = false,
  variant = 'outlined',
  fullWidth = true,
  size = 'small',
  ...otherProps
}) {
  const rule = getValidationRule(fieldName);

  // Si la regla existe y hay patrón, añadirlo a inputProps
  let inputProps = otherProps.inputProps || {};
  if (rule && rule.pattern) {
    inputProps = {
      ...inputProps,
      pattern: rule.pattern,
      maxLength: rule.maxLength,
    };
  } else if (rule && rule.maxLength) {
    inputProps = {
      ...inputProps,
      maxLength: rule.maxLength,
    };
  }

  const deriveCharRegex = (pat) => {
    try {
      const s = String(pat);
      const start = s.indexOf('[');
      const end = s.indexOf(']', start + 1);
      if (start !== -1 && end !== -1) {
        const cls = s.slice(start + 1, end);
        const cleaned = cls.replace(/\\s/g, ' ');
        return new RegExp(`^[${cleaned}]$`, 'u');
      }
    } catch (err) {
    }
    return null;
  };

  const shouldSkipCharFiltering = rule?.type === 'email' || fieldName === 'email' || fieldName === 'createUserEmail';
  const charRegex = !shouldSkipCharFiltering && rule && rule.pattern ? deriveCharRegex(rule.pattern) : null;
  const maxLen = rule && rule.maxLength ? rule.maxLength : null;

  const handleChangeFiltered = (e) => {
    if (!onChange) return;
    const next = e.target.value || '';
    let out = next;
    if (charRegex) {
      out = Array.from(out).filter((ch) => charRegex.test(ch)).join('');
    }
    if (maxLen && out.length > maxLen) {
      out = out.slice(0, maxLen);
    }

    if (out !== next) {
      const syntheticEvent = { ...e, target: { ...e.target, value: out } };
      onChange(syntheticEvent);
      return;
    }

    onChange(e);
  };

  return (
    <TextField
      fullWidth={fullWidth}
      variant={variant}
      size={size}
      disabled={disabled}
      error={touched && error}
      helperText={touched ? helperText : ''}
      value={value}
      onChange={handleChangeFiltered}
      onBlur={onBlur}
      inputProps={inputProps}
      {...otherProps}
    />
  );
}
