import { useEffect, useId, useState } from 'react';
import { Box, Button, TextField } from '@mui/material';
import LabelOutlinedIcon from '@mui/icons-material/LabelOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import {
    createObservationKeyword,
    fetchObservationKeywordById,
    updateObservationKeyword,
} from '../../services/observationKeywords/observationKeywordsService';

const INITIAL_TEXT = '';

export default function ObservationKeywordFormModal({ open, onClose, onSaved, keywordId = null }) {
    const formId = useId();
    const isEdit = Boolean(keywordId);
    const [text, setText] = useState(INITIAL_TEXT);
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);
    const [loadingKeyword, setLoadingKeyword] = useState(isEdit);
    const [alert, setAlert] = useState(null);

    useEffect(() => {
        if (!keywordId) return undefined;

        let cancelled = false;
        const loadKeyword = async () => {
            try {
                const keyword = await fetchObservationKeywordById(keywordId);
                if (cancelled || !keyword) return;
                setText(keyword.text ?? '');
            } catch (loadError) {
                if (!cancelled) {
                    setAlert({ type: 'error', message: getFriendlyApiErrorMessage(loadError, 'No se pudo cargar la palabra clave') });
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
            const saved = isEdit
                ? await updateObservationKeyword(keywordId, { text: trimmed })
                : await createObservationKeyword({ text: trimmed });
            onSaved?.(saved);
            onClose?.();
        } catch (saveError) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(saveError, 'No se pudo guardar la palabra clave') });
        } finally {
            setSaving(false);
        }
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                icon={LabelOutlinedIcon}
                title={isEdit ? 'Editar palabra clave' : 'Agregar palabra clave'}
                subtitle="El texto se compara sin distinguir mayúsculas ni tildes."
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
                        label="Palabra clave"
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
