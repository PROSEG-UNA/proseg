import { useState, useEffect } from 'react';

const INIT = {
    executingUnitId: '', employeeId: '',
    brandId: '', typeId: '', modelId: '',
    campusId: '', buildingId: '', floorNumber: '', locationId: '',
    status: '', decommissionDate: '',
    acquisitionDate: '', warrantyEndDate: '', firmwareSupportEndDate: '',
    ipAddress: '', macAddress: '',
    assetNumber: '', serialNumber: '',
    latitude: '', longitude: '',
};

export function useAssetFormState(open) {
    const [formValues, setFormValues] = useState(INIT);
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [saving, setSaving] = useState(false);
    const [alert, setAlert] = useState(null);
    const [loadingAsset, setLoadingAsset] = useState(false);
    const [assetNumberExists, setAssetNumberExists] = useState(false);
    const [showAssetNumberConfirm, setShowAssetNumberConfirm] = useState(false);
    const [pendingTypeChange, setPendingTypeChange] = useState(null);

    useEffect(() => {
        if (!open) return undefined;

        const resetHandle = window.setTimeout(() => {
            setFormValues(INIT);
            setErrors({});
            setTouched({});
            setSaving(false);
            setAlert(null);
            setAssetNumberExists(false);
            setShowAssetNumberConfirm(false);
            setPendingTypeChange(null);
        }, 0);

        return () => window.clearTimeout(resetHandle);
    }, [open]);

    return {
        formValues,
        setFormValues,
        errors,
        setErrors,
        touched,
        setTouched,
        saving,
        setSaving,
        alert,
        setAlert,
        loadingAsset,
        setLoadingAsset,
        assetNumberExists,
        setAssetNumberExists,
        showAssetNumberConfirm,
        setShowAssetNumberConfirm,
        pendingTypeChange,
        setPendingTypeChange,
    };
}

export const INIT_FORM = INIT;
