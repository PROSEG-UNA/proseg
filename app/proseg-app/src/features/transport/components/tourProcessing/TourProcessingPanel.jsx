import { useState } from 'react';
import { Box, Stack, Typography, useTheme } from '@mui/material';
import AutoFixHighOutlinedIcon from '@mui/icons-material/AutoFixHighOutlined';
import PrimaryButton from '../../../../common/components/PrimaryButton.jsx';
import TourProcessingDraftsTable from './TourProcessingDraftsTable.jsx';
import TourProcessingImportModal from './TourProcessingImportModal.jsx';

export default function TourProcessingPanel() {
    return (
        <Stack spacing={3}>
            <TourProcessingPanelHeader />
            <TourProcessingDraftsTable />
        </Stack>
    );
}

function TourProcessingPanelHeader() {
    const theme = useTheme();
    const t = theme.palette.tones.rose;
    const [importOpen, setImportOpen] = useState(false);

    const handleKeyDown = (event) => {
        if (event.target !== event.currentTarget) return;
        if (event.key !== 'Enter' && event.key !== ' ') return;
        event.preventDefault();
        setImportOpen(true);
    };

    return (
        <>
            <Box
                role="button"
                tabIndex={0}
                aria-label="Importar archivo"
                onClick={() => setImportOpen(true)}
                onKeyDown={handleKeyDown}
                sx={{
                    position: 'relative',
                    overflow: 'hidden',
                    cursor: 'pointer',
                    outline: 'none',
                    borderRadius: '16px',
                    border: '1px solid',
                    borderColor: 'divider',
                    background: `linear-gradient(180deg, color-mix(in srgb, ${theme.vars.palette.background.paperWarm} 95%, transparent) 0%, color-mix(in srgb, ${theme.vars.palette.background.paperWarm} 78%, transparent) 100%)`,
                    boxShadow: t.shadowResting,
                    transition: 'box-shadow 0.22s ease, border-color 0.22s ease',
                    '&:hover': { borderColor: t.ring, boxShadow: t.shadowHover },
                    '&:focus-visible': { borderColor: t.fg, boxShadow: `0 0 0 3px color-mix(in srgb, ${t.fg} 30%, transparent)` },
                }}
            >
                <Box
                    sx={{
                        position: 'absolute', inset: 0, pointerEvents: 'none',
                        background: `radial-gradient(circle at 100% 0%, ${t.glow} 0%, transparent 55%)`,
                        opacity: 0.9,
                    }}
                />
    
                <Stack sx={{ position: 'relative', zIndex: 1, p: { xs: 2.5, md: 3 } }}>
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
                            tabIndex={-1}
                            sx={{ flexShrink: 0, alignSelf: { xs: 'flex-end', sm: 'center' } }}
                        >
                            Importar archivo
                        </PrimaryButton>
                    </Stack>
                </Stack>
            </Box>
            <TourProcessingImportModal open={importOpen} onClose={() => setImportOpen(false)} />
        </>
    );
}
