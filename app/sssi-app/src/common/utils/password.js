export function getPasswordStrength(password) {
  if (!password) return { score: 0, label: '', color: 'transparent' };
  let score = 0;
  if (password.length >= 8) score++;
  if (password.length >= 12) score++;
  if (/[A-Z]/.test(password)) score++;
  if (/[0-9]/.test(password)) score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;
  if (score <= 1) return { score: 20, label: 'Muy débil', color: '#ef4444' };
  if (score === 2) return { score: 40, label: 'Débil', color: '#f59e0b' };
  if (score === 3) return { score: 60, label: 'Regular', color: '#d97706' };
  if (score === 4) return { score: 80, label: 'Fuerte', color: '#059669' };
  return { score: 100, label: 'Muy fuerte', color: '#047857' };
}

export function validatePasswordChange({
  currentPassword = '',
  password = '',
  confirmPassword = '',
  requireCurrentPassword = false,
}) {
  if (requireCurrentPassword && !currentPassword.trim()) {
    return 'La contraseña actual es requerida.';
  }

  if (!password.trim()) {
    return 'La nueva contraseña es requerida.';
  }

  if (password.length < 8) {
    return 'La nueva contraseña debe tener al menos 8 caracteres.';
  }

  if (!/[A-Z]/.test(password)) {
    return 'La nueva contraseña debe incluir al menos una letra mayúscula.';
  }

  if (!/[0-9]/.test(password)) {
    return 'La nueva contraseña debe incluir al menos un número.';
  }

  if (!/[^A-Za-z0-9]/.test(password)) {
    return 'La nueva contraseña debe incluir al menos un carácter especial.';
  }

  if (requireCurrentPassword && currentPassword === password) {
    return 'La nueva contraseña no puede ser igual a la actual.';
  }

  if (!confirmPassword.trim()) {
    return 'La confirmación de la nueva contraseña es requerida.';
  }

  if (password !== confirmPassword) {
    return 'La nueva contraseña y su confirmación no coinciden.';
  }

  return '';
}
