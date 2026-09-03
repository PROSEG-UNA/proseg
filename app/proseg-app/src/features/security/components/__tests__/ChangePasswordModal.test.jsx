import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { AuthContext } from '../../../../common/context/AuthContext';
import AppTheme from '../../../../common/theme/AppTheme';
import { ChangePasswordModal } from '../ChangePasswordModal';
import { changePassword } from '../../../auth/services/authService';

vi.mock('../../../auth/services/authService', () => ({
  changePassword: vi.fn(),
}));

const refreshAuth = vi.fn();

function renderModal(props = {}) {
  return render(
    <AppTheme defaultColorScheme="light">
      <AuthContext.Provider
        value={{
          refreshAuth,
        }}
      >
        <ChangePasswordModal open onClose={vi.fn()} {...props} />
      </AuthContext.Provider>
    </AppTheme>,
  );
}

function getField(label) {
  return screen.getAllByLabelText(label, { selector: 'input' })[0];
}

async function fillField(label, value) {
  const field = getField(label);
  fireEvent.change(field, { target: { value } });
  await waitFor(() => expect(field).toHaveValue(value));
}

beforeEach(() => {
  refreshAuth.mockReset();
  changePassword.mockReset();
});

describe('ChangePasswordModal', () => {
  it('muestra campos, valida y permite cerrar', async () => {
    const onClose = vi.fn();
    render(
      <AppTheme defaultColorScheme="light">
        <AuthContext.Provider value={{ refreshAuth }}>
          <ChangePasswordModal open onClose={onClose} />
        </AuthContext.Provider>
      </AppTheme>,
    );

    expect(screen.getByText('Seguridad de la cuenta')).toBeInTheDocument();
    expect(screen.getByLabelText('Contraseña actual')).toBeInTheDocument();
    expect(screen.getByLabelText('Nueva contraseña')).toBeInTheDocument();
    expect(screen.getByLabelText('Confirmar nueva contraseña')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('muestra validación cuando las contraseñas no coinciden', async () => {
    const user = userEvent.setup();
    renderModal();

    await fillField('Contraseña actual', 'Actual#123');
    await fillField('Nueva contraseña', 'Nueva#123');
    await fillField('Confirmar nueva contraseña', 'Nueva#999');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(screen.getByText('La nueva contraseña y su confirmación no coinciden.')).toBeInTheDocument());
    expect(changePassword).not.toHaveBeenCalled();
  });

  it('envía el cambio exitosamente', async () => {
    changePassword.mockResolvedValue({ data: null });
    const user = userEvent.setup();

    renderModal();

    await fillField('Contraseña actual', 'Actual#123');
    await fillField('Nueva contraseña', 'Nueva#123');
    await fillField('Confirmar nueva contraseña', 'Nueva#123');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(changePassword).toHaveBeenCalledTimes(1));
    expect(refreshAuth).toHaveBeenCalledTimes(1);
    expect(screen.getByText('Contraseña actualizada correctamente.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cerrar' })).toBeInTheDocument();
  });

  it('muestra el error del servidor', async () => {
    changePassword.mockRejectedValue({
      response: { data: { message: 'Credenciales incorrectas' } },
    });
    const user = userEvent.setup();

    renderModal();

    await fillField('Contraseña actual', 'Actual#123');
    await fillField('Nueva contraseña', 'Nueva#123');
    await fillField('Confirmar nueva contraseña', 'Nueva#123');
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    await waitFor(() => expect(screen.getByText('Credenciales incorrectas')).toBeInTheDocument());
  });

  it('previene envíos duplicados mientras está cargando', async () => {
    const user = userEvent.setup();
    let resolveRequest;
    const pendingRequest = new Promise((resolve) => {
      resolveRequest = resolve;
    });
    changePassword.mockReturnValue(pendingRequest);

    renderModal();

    await fillField('Contraseña actual', 'Actual#123');
    await fillField('Nueva contraseña', 'Nueva#123');
    await fillField('Confirmar nueva contraseña', 'Nueva#123');

    const submitButton = screen.getByRole('button', { name: 'Guardar cambios' });
    await user.click(submitButton);

    await waitFor(() => expect(submitButton).toBeDisabled());
    await user.click(submitButton);

    expect(changePassword).toHaveBeenCalledTimes(1);
    resolveRequest({ data: null });
    await waitFor(() => expect(refreshAuth).toHaveBeenCalledTimes(1));
  });
});
