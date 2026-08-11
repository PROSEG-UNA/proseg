import { useState, useEffect, useCallback } from 'react';

const createEmptyComponent = () => ({
    localId: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
    id: null,
    name: '',
    quantity: '1',
    location: '',
    observations: '',
});

export function useAssetComponents(open) {
    const [components, setComponents] = useState([]);
    const [componentErrors, setComponentErrors] = useState({});
    const [componentsToDelete, setComponentsToDelete] = useState([]);

    const resetComponents = useCallback(() => {
        setComponents([]);
        setComponentErrors({});
        setComponentsToDelete([]);
    }, []);

    const scheduleComponentsReset = useCallback(() => {
        if (!open) return undefined;

        const resetHandle = window.setTimeout(resetComponents, 0);

        return () => window.clearTimeout(resetHandle);
    }, [open, resetComponents]);

    useEffect(scheduleComponentsReset, [scheduleComponentsReset]);

    const addComponent = () => {
        setComponents(prev => [...prev, createEmptyComponent()]);
    };

    const updateComponentField = (localId, key, value) => {
        setComponents(prev => prev.map(component => component.localId === localId
            ? { ...component, [key]: value }
            : component));

        setComponentErrors(prev => {
            if (!prev[localId]?.[key]) return prev;
            return {
                ...prev,
                [localId]: {
                    ...prev[localId],
                    [key]: '',
                },
            };
        });
    };

    const removeComponent = (localId) => {
        setComponents(prev => {
            const component = prev.find(item => item.localId === localId);
            if (component?.id) {
                setComponentsToDelete(current => [...current, component.id]);
            }
            return prev.filter(item => item.localId !== localId);
        });

        setComponentErrors(prev => {
            if (!prev[localId]) return prev;
            const next = { ...prev };
            delete next[localId];
            return next;
        });
    };

    const validateComponents = () => {
        const nextErrors = {};

        components.forEach((component) => {
            const errorsByField = {};

            if (!component.name?.trim()) {
                errorsByField.name = 'El nombre es obligatorio';
            } else if (component.name.trim().length > 255) {
                errorsByField.name = 'El nombre no puede superar los 255 caracteres';
            }

            const quantity = Number(component.quantity);
            if (!component.quantity?.toString().trim()) {
                errorsByField.quantity = 'La cantidad es obligatoria';
            } else if (!Number.isInteger(quantity) || quantity < 1) {
                errorsByField.quantity = 'La cantidad debe ser mayor o igual a 1';
            }

            if ((component.location ?? '').length > 255) {
                errorsByField.location = 'La ubicación no puede superar los 255 caracteres';
            }

            if ((component.observations ?? '').length > 1000) {
                errorsByField.observations = 'Las observaciones no pueden superar los 1000 caracteres';
            }

            if (Object.keys(errorsByField).length > 0) {
                nextErrors[component.localId] = errorsByField;
            }
        });

        setComponentErrors(nextErrors);
        return Object.keys(nextErrors).length === 0;
    };

    return {
        components,
        setComponents,
        componentErrors,
        setComponentErrors,
        componentsToDelete,
        setComponentsToDelete,
        addComponent,
        updateComponentField,
        removeComponent,
        validateComponents,
    };
}
