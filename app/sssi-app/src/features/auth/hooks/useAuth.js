import { useState } from 'react';
import { login, register } from '../services/authService';
import { useNavigate } from 'react-router-dom';

export function useAuth() {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const handleLogin = async (identifier, password) => {
        setLoading(true);
        setError('');
        try {
            await login(identifier, password);
            navigate('/inventario');
        } catch (err) {
            console.error('[useAuth] login error:', err?.response?.status, err?.response?.data?.errors, err?.message);
            setError('Error en la autenticación. Intenta de nuevo.');
        } finally {
            setLoading(false);
        }
    };

    const handleRegister = async ({ username, password, email, firstName, lastName }) => {
        setLoading(true);
        setError('');
        try {
            await register(username, password, email, firstName, lastName);
            navigate('/login');
        } catch {
            setError('Error al registrar el usuario. Intenta de nuevo.');
        } finally {
            setLoading(false);
        }
    };

    return { loading, error, handleLogin, handleRegister };
}