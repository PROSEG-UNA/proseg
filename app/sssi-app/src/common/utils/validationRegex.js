export const REGEX_PATTERNS = {
  USERNAME: '[a-zA-Z0-9_-]{3,24}',
  EMAIL: "[^\\s@]+@[^\\s@]+\\.[^\\s@]+",
  PASSWORD: "[a-zA-Z0-9!@#$%^&*()_+=;:'\",.<>/?|~`-]{8,24}",
  IDENTIFIER: '(?:[a-zA-Z0-9_-]{3,24}|[^\\s@]+@[^\\s@]+\\.[^\\s@]+)',
  ROLE_NAME: '[a-zA-Z0-9 _-]{3,24}',
  NAME: '[a-zA-Z\\sáéíóúñÁÉÍÓÚÑ]{2,24}',
  DESCRIPTION: "[a-zA-Z0-9\\s\\-_.,;:'()\"áéíóúñÁÉÍÓÚÑ]{0,150}",
};

export const VALIDATION_RULES = {
  identifier: {
    label: 'Usuario o Email',
    pattern: REGEX_PATTERNS.IDENTIFIER,
    minLength: 3,
    maxLength: 64,
    required: true,
    errorMessage: 'Debe ser un usuario de 3-64 caracteres o un email válido',
  },
  loginPassword: {
    label: 'Contraseña',
    pattern: REGEX_PATTERNS.PASSWORD,
    minLength: 8,
    maxLength: 24,
    required: true,
    errorMessage: 'La contraseña debe tener entre 8 y 24 caracteres y solo usar caracteres permitidos',
    type: 'password',
  },
  username: {
    label: 'Nombre de usuario',
    pattern: REGEX_PATTERNS.USERNAME,
    minLength: 3,
    maxLength: 24,
    required: true,
    errorMessage: 'Solo puede contener letras, números, guiones y guiones bajos (3-24)'
  },
  email: {
    label: 'Correo electrónico',
    pattern: REGEX_PATTERNS.EMAIL,
    required: true,
    maxLength: 64,
    errorMessage: 'Ingrese un email válido (máx 64 caracteres)',
    type: 'email',
  },
  registerPassword: {
    label: 'Contraseña',
    pattern: REGEX_PATTERNS.PASSWORD,
    minLength: 8,
    maxLength: 24,
    required: true,
    errorMessage: 'La contraseña debe tener entre 8 y 24 caracteres y solo usar caracteres permitidos',
    type: 'password',
  },
  firstName: {
    label: 'Nombre',
    pattern: REGEX_PATTERNS.NAME,
    minLength: 2,
    maxLength: 24,
    required: true,
    errorMessage: 'Solo letras y espacios (2-24 caracteres)',
  },
  lastName: {
    label: 'Apellido',
    pattern: REGEX_PATTERNS.NAME,
    minLength: 2,
    maxLength: 24,
    required: true,
    errorMessage: 'Solo letras y espacios (2-24 caracteres)',
  },
  createUserUsername: {
    label: 'Nombre de usuario',
    pattern: REGEX_PATTERNS.USERNAME,
    minLength: 3,
    maxLength: 24,
    required: true,
    errorMessage: 'Solo puede contener letras, números, guiones y guiones bajos (3-24)',
  },
  createUserEmail: {
    label: 'Correo electrónico',
    pattern: REGEX_PATTERNS.EMAIL,
    required: true,
    maxLength: 64,
    errorMessage: 'Ingrese un email válido',
    type: 'email',
  },
  createUserFirstName: {
    label: 'Nombre',
    pattern: REGEX_PATTERNS.NAME,
    minLength: 2,
    maxLength: 24,
    required: true,
    errorMessage: 'Solo letras y espacios (2-24 caracteres)',
  },
  createUserLastName: {
    label: 'Apellido',
    pattern: REGEX_PATTERNS.NAME,
    minLength: 2,
    maxLength: 24,
    required: true,
    errorMessage: 'Solo letras y espacios (2-24 caracteres)',
  },
  roleName: {
    label: 'Nombre del rol',
    pattern: REGEX_PATTERNS.ROLE_NAME,
    minLength: 3,
    maxLength: 24,
    required: true,
    errorMessage: 'El nombre del rol debe tener entre 3 y 24 caracteres y solo usar letras, números, espacios, guiones y guiones bajos',
  },
  roleDescription: {
    label: 'Descripción',
    pattern: REGEX_PATTERNS.DESCRIPTION,
    maxLength: 150,
    required: false,
    errorMessage: 'La descripción solo puede contener letras, números, espacios y puntuación básica (máx 150 caracteres)',
  },
};
export function validateField(value, rule) {
  if (!value && rule.required) {
    return { isValid: false, error: `${rule.label} es requerido` };
  }

  if (!value && !rule.required) {
    return { isValid: true, error: '' };
  }

  if (rule.minLength && value.length < rule.minLength) {
    return {
      isValid: false,
      error: `${rule.label} debe tener mínimo ${rule.minLength} caracteres`,
    };
  }

  if (rule.maxLength && value.length > rule.maxLength) {
    return {
      isValid: false,
      error: `${rule.label} debe tener máximo ${rule.maxLength} caracteres`,
    };
  }

  if (rule.pattern) {
    const pat = typeof rule.pattern === 'string' ? rule.pattern : String(rule.pattern);
    const hasAnchors = pat.startsWith('^') || pat.endsWith('$');
    const regex = hasAnchors ? new RegExp(pat) : new RegExp(`^${pat}$`);
    if (!regex.test(value)) {
      return { isValid: false, error: rule.errorMessage };
    }
  }

  return { isValid: true, error: '' };
}
export function getValidationRule(fieldName) {
  return VALIDATION_RULES[fieldName] || null;
}
export function validateForm(formData, fieldNames) {
  const errors = {};
  let isFormValid = true;

  fieldNames.forEach((fieldName) => {
    const rule = getValidationRule(fieldName);
    if (rule) {
      const { isValid, error } = validateField(formData[fieldName] || '', rule);
      if (!isValid) {
        errors[fieldName] = error;
        isFormValid = false;
      }
    }
  });

  return { isFormValid, errors };
}
