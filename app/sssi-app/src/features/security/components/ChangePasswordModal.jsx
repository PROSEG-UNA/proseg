import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Divider,
  IconButton,
  InputAdornment,
  LinearProgress,
  TextField,
  Typography,
} from '@mui/material';
import LockResetIcon from '@mui/icons-material/LockReset';
import Visibility from '@mui/icons-material/Visibility';
import VisibilityOff from '@mui/icons-material/VisibilityOff';
import GeneralModal from '../../../common/components/GeneralModal';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { getPasswordStrength, validatePasswordChange } from '../../../common/utils/password';
import { changePassword } from '../../auth/services/authService';
import { useContext } from 'react';
import { AuthContext } from '../../../common/context/AuthContext';

export function ChangePasswordModal({ open, onClose }) {
  const { refreshAuth } = useContext(AuthContext);
  const [currentPassword, setCurrentPassword] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrent, setShowCurrent] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    if (!open) {
      setCurrentPassword('');
      setPassword('');
      setConfirmPassword('');
      setShowCurrent(false);
      setShowPassword(false);
      setShowConfirm(false);
      setLoading(false);
      setError('');
      setSuccess('');
    }
  }, [open]);

  const strength = useMemo(() => getPasswordStrength(password), [password]);

  const resetSensitiveFields = () => {
    setCurrentPassword('');
    setPassword('');
    setConfirmPassword('');
    setShowCurrent(false);
    setShowPassword(false);
    setShowConfirm(false);
  };

  const handleSubmit = async () => {
    if (loading) return;

    const validationMessage = validatePasswordChange({
      currentPassword,
      password,
      confirmPassword,
      requireCurrentPassword: true,
    });

    if (validationMessage) {
      setError(validationMessage);
      setSuccess('');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      await changePassword(currentPassword, password, confirmPassword);
      await refreshAuth();
      resetSensitiveFields();
      setSuccess('Contraseña actualizada correctamente.');
    } catch (apiError) {
      setError(getFriendlyApiErrorMessage(apiError, 'No se pudo cambiar la contraseña.'));
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    resetSensitiveFields();
    setError('');
    setSuccess('');
    onClose();
  };

  return (
    <GeneralModal
      open={open}
      onClose={handleClose}
      maxWidth="xs"
      fullScreenAt="sm"
      icon={LockResetIcon}
      title="Cambiar contraseña"
      subtitle="Actualizá tus credenciales de acceso"
      secondaryButton={success ? null : { label: 'Cancelar', onClick: handleClose, disabled: loading }}
      primaryButton={{
        label: success ? 'Cerrar' : 'Guardar cambios',
        onClick: success ? handleClose : undefined,
        type: success ? 'button' : 'submit',
        form: 'change-password-form',
        disabled: loading,
        loading,
        sx: { minWidth: 140 },
      }}
    >
      <Box
        component="form"
        id="change-password-form"
        onSubmit={(event) => {
          event.preventDefault();
          if (!success) {
            void handleSubmit();
          }
        }}
        sx={{ px: 3, pt: 2.5, pb: 3, display: 'grid', gap: 2 }}
      >
        <Box sx={{ display: 'grid', gap: 1, color: 'text.secondary' }}>
          <Typography sx={{ fontSize: '0.92rem', fontWeight: 600, color: 'text.primary' }}>
            Seguridad de la cuenta
          </Typography>
          <Typography sx={{ fontSize: '0.82rem', lineHeight: 1.6 }}>
            Podés actualizar tu contraseña en cualquier momento sin salir del sistema.
          </Typography>
        </Box>

        <Divider />

        {error && <Alert severity="error">{error}</Alert>}
        {success && <Alert severity="success">{success}</Alert>}

        <TextField
          label="Contraseña actual"
          type={showCurrent ? 'text' : 'password'}
          value={currentPassword}
          onChange={(event) => setCurrentPassword(event.target.value)}
          disabled={loading || Boolean(success)}
          fullWidth
          size="small"
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                <IconButton
                  onClick={() => setShowCurrent((value) => !value)}
                  edge="end"
                  size="small"
                  aria-label={showCurrent ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  disabled={loading || Boolean(success)}
                >
                  {showCurrent ? <VisibilityOff fontSize="small" /> : <Visibility fontSize="small" />}
                </IconButton>
              </InputAdornment>
            ),
          }}
        />

        <TextField
          label="Nueva contraseña"
          type={showPassword ? 'text' : 'password'}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          disabled={loading || Boolean(success)}
          fullWidth
          size="small"
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                <IconButton
                  onClick={() => setShowPassword((value) => !value)}
                  edge="end"
                  size="small"
                  aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  disabled={loading || Boolean(success)}
                >
                  {showPassword ? <VisibilityOff fontSize="small" /> : <Visibility fontSize="small" />}
                </IconButton>
              </InputAdornment>
            ),
          }}
        />

        {password && (
          <Box sx={{ display: 'grid', gap: 0.75 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography sx={{ fontSize: 11, fontWeight: 700, color: 'text.secondary', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                Fortaleza
              </Typography>
              <Typography sx={{ fontSize: 11, fontWeight: 700, color: strength.color }}>
                {strength.label}
              </Typography>
            </Box>
            <LinearProgress variant="determinate" value={strength.score} sx={{ height: 4, borderRadius: 2 }} />
            <Typography sx={{ fontSize: 11.5, color: 'text.secondary', lineHeight: 1.5 }}>
              Debe incluir mayúscula, número, carácter especial y al menos 8 caracteres.
            </Typography>
          </Box>
        )}

        <TextField
          label="Confirmar nueva contraseña"
          type={showConfirm ? 'text' : 'password'}
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
          disabled={loading || Boolean(success)}
          fullWidth
          size="small"
          InputProps={{
            endAdornment: (
              <InputAdornment position="end">
                <IconButton
                  onClick={() => setShowConfirm((value) => !value)}
                  edge="end"
                  size="small"
                  aria-label={showConfirm ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                  disabled={loading || Boolean(success)}
                >
                  {showConfirm ? <VisibilityOff fontSize="small" /> : <Visibility fontSize="small" />}
                </IconButton>
              </InputAdornment>
            ),
          }}
        />
      </Box>
    </GeneralModal>
  );
}
