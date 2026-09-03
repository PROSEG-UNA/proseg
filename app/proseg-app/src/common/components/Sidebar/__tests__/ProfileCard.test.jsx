import { render, screen, fireEvent, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { AuthContext } from '../../../context/AuthContext';
import AppTheme from '../../../theme/AppTheme';
import { ProfileCard } from '../ProfileCard';

vi.mock('../../../../features/auth/services/authService', () => ({
  updateCurrentUserProfileImage: vi.fn(),
  changePassword: vi.fn(),
}));

vi.mock('../../../../features/security/components/ChangePasswordModal', () => ({
  ChangePasswordModal: ({ open, onClose }) => (
    open ? (
      <div role="dialog" aria-label="Cambiar contraseña">
        <button type="button" onClick={onClose}>Cerrar diálogo</button>
      </div>
    ) : null
  ),
}));

describe('ProfileCard', () => {
  it('muestra la opción de cambiar contraseña desde el perfil', () => {
    const user = {
      id: 'user-1',
      username: 'jperez',
      firstName: 'Juan',
      lastName: 'Pérez',
      email: 'juan@una.cr',
      permissions: ['ROLE_USER'],
    };

    const { container } = render(
      <AppTheme defaultColorScheme="light">
        <AuthContext.Provider value={{ refreshAuth: vi.fn() }}>
          <ProfileCard user={user} />
        </AuthContext.Provider>
      </AppTheme>,
    );

    fireEvent.click(within(container).getByRole('button'));

    expect(screen.getByText('Seguridad')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cambiar contraseña' })).toBeInTheDocument();
  });

  it('abre el modal para cambiar contraseña desde el perfil', async () => {
    const user = {
      id: 'user-1',
      username: 'jperez',
      firstName: 'Juan',
      lastName: 'Pérez',
      email: 'juan@una.cr',
      permissions: ['ROLE_USER'],
    };

    const { container } = render(
      <AppTheme defaultColorScheme="light">
        <AuthContext.Provider value={{ refreshAuth: vi.fn() }}>
          <ProfileCard user={user} />
        </AuthContext.Provider>
      </AppTheme>,
    );

    const userEventApi = userEvent.setup();
    fireEvent.click(within(container).getByRole('button'));
    await userEventApi.click(await screen.findByRole('button', { name: 'Cambiar contraseña' }));

    await screen.findByRole('button', { name: 'Guardar cambios' });
    expect(screen.getAllByRole('dialog')).toHaveLength(2);
  });
});
