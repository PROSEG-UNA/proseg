import { Fragment, useState } from 'react';
import { Box, Stack, Typography, useTheme } from '@mui/material';
import AutoFixHighOutlinedIcon from '@mui/icons-material/AutoFixHighOutlined';
import PrimaryButton from '../../../../common/components/PrimaryButton.jsx';
import CleaningWizardModal from './CleaningWizardModal.jsx';

const WIZARD_STEPS = [
    { label: 'Archivo y parámetros' },
    { label: 'Vista previa' },
    { label: 'Confirmación' },
    { label: 'Resultado' },
];

export default function CleaningPanel({ onImported }) {
    const [wizardOpen, setWizardOpen] = useState(false);
    const theme = useTheme();
    const t = theme.palette.tones.rose;

    return (
        <>
            <Box
                sx={{
                    position: 'relative',
                    overflow: 'hidden',
                    borderRadius: '16px',
                    border: '1px solid',
                    borderColor: 'divider',
                    background: `linear-gradient(180deg, color-mix(in srgb, ${theme.vars.palette.background.paperWarm} 95%, transparent) 0%, color-mix(in srgb, ${theme.vars.palette.background.paperWarm} 78%, transparent) 100%)`,
                    boxShadow: t.shadowResting,
                    transition: 'box-shadow 0.22s ease, border-color 0.22s ease',
                    '&:hover': { borderColor: t.ring, boxShadow: t.shadowHover },
                }}
            >
                <Box
                    sx={{
                        position: 'absolute', inset: 0, pointerEvents: 'none',
                        background: `radial-gradient(circle at 100% 0%, ${t.glow} 0%, transparent 55%)`,
                        opacity: 0.9,
                    }}
                />

                <Stack spacing={3} sx={{ position: 'relative', zIndex: 1, p: { xs: 2.5, md: 3 } }}>
                    <Stack
                        direction={{ xs: 'column', sm: 'row' }}
                        spacing={2}
                        alignItems={{ xs: 'flex-start', sm: 'center' }}
                        justifyContent="space-between"
                    >
                        <Stack direction="row" spacing={2} alignItems="center" sx={{ flex: 1, minWidth: 0 }}>
                            <Box
                                sx={{
                                    width: 48, height: 48, borderRadius: '13px', flexShrink: 0,
                                    background: t.soft,
                                    border: `1px solid ${t.ring}`,
                                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                                    color: t.fg,
                                }}
                            >
                                <AutoFixHighOutlinedIcon sx={{ fontSize: 24 }} />
                            </Box>
                            <Box>
                                <Typography sx={{ fontWeight: 800, fontSize: 15.5, color: 'text.primary', lineHeight: 1.2 }}>
                                    Depuración de giras
                                </Typography>
                                <Typography sx={{ color: 'text.secondary', fontSize: 13.5, mt: 0.3 }}>
                                    Carga, revisa y registra giras depuradas desde el previo de comisión.
                                </Typography>
                            </Box>
                        </Stack>

                        <PrimaryButton
                            onClick={() => setWizardOpen(true)}
                            sx={{ flexShrink: 0, alignSelf: { xs: 'flex-end', sm: 'center' } }}
                        >
                            Iniciar depuración
                        </PrimaryButton>
                    </Stack>

                    <Box sx={{ display: 'flex', alignItems: 'flex-start' }}>
                        {WIZARD_STEPS.map((step, index) => (
                            <Fragment key={step.label}>
                                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 0.75, flex: '0 0 auto' }}>
                                    <Box sx={{
                                        width: 26, height: 26, borderRadius: '50%',
                                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                                        bgcolor: 'transparent', border: '2px solid', borderColor: 'divider',
                                    }}>
                                        <Typography component="span" sx={{ fontSize: 11, fontWeight: 800, color: 'text.disabled', lineHeight: 1 }}>
                                            {index + 1}
                                        </Typography>
                                    </Box>
                                    <Typography sx={{ fontSize: 10.5, color: 'text.disabled', whiteSpace: 'nowrap' }}>
                                        {step.label}
                                    </Typography>
                                </Box>
                                {index < WIZARD_STEPS.length - 1 && (
                                    <Box sx={{ flex: 1, height: 2, mt: '12px', bgcolor: 'divider', minWidth: 8 }} />
                                )}
                            </Fragment>
                        ))}
                    </Box>
                </Stack>
            </Box>

            <CleaningWizardModal
                open={wizardOpen}
                onClose={() => setWizardOpen(false)}
                onImported={onImported}
            />
        </>
    );
}
