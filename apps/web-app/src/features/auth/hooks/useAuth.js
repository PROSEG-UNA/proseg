import { useState, useContext } from 'react';
import { login, register, logout } from '../services/authService';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../../../common/context/AuthContext';

export function useAuth() {
    const navigate = useNavigate();
    const { logout: logoutAuth, setIsAuthenticated } = useContext(AuthContext);
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
            setIsAuthenticated(true);
            navigate('/home');
        } catch (err) {
            const message = err.response?.data?.message || err.message || 'Error al iniciar sesión';
            setAlert({ type: 'error', message });
        } finally {
            setLoading(false);
        }
    };

    const handleRegister = async ({ username, password, registerPassword, email, firstName, lastName, captchaToken }) => {
        setLoading(true);
        setAlert(null);
        try {
            await register({
                username,
                password: password || registerPassword,
                email,
                firstName,
                lastName,
                captchaToken,
            });
            navigate('/login');
        } catch (err) {
            const message = err.response?.data?.message || err.message || 'Error al registrar el usuario';
            setAlert({ type: 'error', message });
        } finally {
            setLoading(false);
        }
    };

    const handleLogout = async () => {
        setLoading(true);
        setAlert(null);
        try {
            await logout();
            await logoutAuth();
            window.location.href = '/login';
        } catch (err) {
            const message = err.response?.data?.message || err.message || 'Error al cerrar sesión. Intenta de nuevo.';
            setAlert({ type: 'error', message });
            await logoutAuth();
            window.location.reload();
        } finally {
            setLoading(false);
        }
    };

    return { loading, alert, handleAlertClose, handleLogin, handleRegister, handleLogout };
}