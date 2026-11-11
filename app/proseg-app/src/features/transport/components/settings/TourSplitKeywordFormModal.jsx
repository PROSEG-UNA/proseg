import { useEffect, useId, useState } from 'react';
import { Box, Button, TextField } from '@mui/material';
import PlaceOutlinedIcon from '@mui/icons-material/PlaceOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import {
    createTourSplitKeyword,
    fetchTourSplitKeywordById,
    updateTourSplitKeyword,
} from '../../services/tourSplit/tourSplitService';

export default function TourSplitKeywordFormModal({ open, onClose, onSaved, mode, keywordId = null }) {
    const formId = useId();
    const isEdit = Boolean(keywordId);
    const [text, setText] = useState('');
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);
    const [loadingKeyword, setLoadingKeyword] = useState(isEdit);
    const [alert, setAlert] = useState(null);

    useEffect(() => {
        if (!keywordId) return undefined;

        let cancelled = false;
        const loadKeyword = async () => {
            try {
                const keyword = await fetchTourSplitKeywordById(keywordId);
                if (cancelled || !keyword) return;
                setText(keyword.text ?? '');
            } catch (loadError) {
                if (!cancelled) {
                    setAlert({ type: 'error', message: getFriendlyApiErrorMessage(loadError, 'No se pudo cargar el destino') });
                }
            } finally {
                if (!cancelled) setLoadingKeyword(false);
            }
        };

        void loadKeyword();
        return () => {
            cancelled = true;
        };
    }, [keywordId]);

    const handleSave = async (event) => {
        event.preventDefault();
        if (saving || loadingKeyword) return;
        const trimmed = text.trim();
        if (!trimmed) {
            setError('Este campo es requerido');
            return;
        }
        setSaving(true);
        try {
            const payload = { text: trimmed, mode };
            const saved = isEdit
                ? await updateTourSplitKeyword(keywordId, payload)
                : await createTourSplitKeyword(payload);
            onSaved?.(saved);
            onClose?.();
        } catch (saveError) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(saveError, 'No se pudo guardar el destino') });
        } finally {
            setSaving(false);
        }
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                icon={PlaceOutlinedIcon}
                title={isEdit ? 'Editar destino' : 'Agregar destino'}
                subtitle="Escribe el destino o parte de él, por ejemplo Cartago. Basta con que el destino de la gira lo contenga. No se distinguen mayúsculas ni tildes."
                loading={loadingKeyword}
                footerLeft={
                    <Button
                        type="button"
                        variant="outlined"
                        size="small"
                        onClick={onClose}
                        disabled={saving}
                        sx={{ borderColor: 'divider', color: 'text.secondary', textTransform: 'none', fontWeight: 600, fontSize: 12.5, borderRadius: '8px' }}
                    >
                        Cancelar
                    </Button>
                }
                primaryButton={{
                    label: isEdit ? 'Guardar cambios' : 'Agregar',
                    type: 'submit',
                    form: formId,
                    loading: saving,
                    disabled: saving || loadingKeyword,
                }}
            >
                <Box component="form" id={formId} onSubmit={handleSave} sx={{ p: 3 }}>
                    <TextField
                        label="Destino"
                        value={text}
                        onChange={(event) => {
                            setText(event.target.value);
                            if (error) setError('');
                        }}
                        error={Boolean(error)}
                        helperText={error || ' '}
                        required
                        fullWidth
                        autoFocus
                    />
                </Box>
            </GeneralModal>
            <DialogModal
                open={Boolean(alert)}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}
