import { useState } from 'react';
import { login, register, logout } from '../services/authService';
import { useNavigate } from 'react-router-dom';

export function useAuth() {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [alert, setAlert] = useState(null);

    const handleAlertClose = () => {
        setAlert(null);
    };

    const handleLogin = async (identifier, password) => {
        setLoading(true);
        setAlert(null);
        try {
            await login(identifier, password);
            navigate('/home');
        } catch (err) {
            setAlert({ type: 'error', message: err instanceof Error ? err.message : 'Error al iniciar sesión' });
        } finally {
            setLoading(false);
        }
    };

    const handleRegister = async ({ username, password, email, firstName, lastName }) => {
        setLoading(true);
        setAlert(null);
        try {
            await register(username, password, email, firstName, lastName);
            navigate('/login');
        } catch (err) {
            setAlert({ type: 'error', message: err instanceof Error ? err.message : 'Error al registrar el usuario' });
        } finally {
            setLoading(false);
        }
    };

    const handleLogout = async () => {
        setLoading(true);
        setAlert(null);
        try {
            await logout();
            navigate('/login');
        } catch (err) {
            setAlert({ type: 'error', message: err instanceof Error ? err.message : 'Error al cerrar sesión. Intenta de nuevo.' });
        } finally {
            setLoading(false);
        }
    };

    return { loading, alert, handleAlertClose, handleLogin, handleRegister, handleLogout };
}