export function getFriendlyApiErrorMessage(error, fallbackMessage = 'Ocurrió un error inesperado') {
  const status = error?.response?.status;

  if (status === 403) {
    return 'No cuentas con permisos suficientes para realizar esta acción.';
  }

  return error?.response?.data?.message || error?.message || fallbackMessage;
}