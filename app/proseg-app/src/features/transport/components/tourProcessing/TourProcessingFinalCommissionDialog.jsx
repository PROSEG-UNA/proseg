import { useState } from 'react';
import { Alert, Box, Checkbox, FormControlLabel, Stack, Typography } from '@mui/material';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import { FINAL_COMMISSION_SECTIONS, REJECTIONS_KEY, finalCommissionIssueKeys } from './finalCommissionIssues.js';

const REJECTIONS_LABEL = 'Entiendo que estas giras cambiarán a estado No aprobada';

function IssueCard({ issue, color, acknowledgeLabel, acknowledged, onToggle }) {
    return (
        <Box sx={{ border: '1px solid', borderColor: `${color}.main`, borderRadius: '10px', p: 1.5, bgcolor: 'background.paper' }}>
            <Typography sx={{ fontWeight: 700, fontSize: 13.5, color: `${color}.main` }}>
                {issue.title}
            </Typography>
            <Box component="ul" sx={{ m: 0, mt: 0.75, pl: 2.5, maxHeight: 168, overflowY: 'auto', fontSize: 12.5, color: 'text.secondary' }}>
                {issue.details.map((detail, index) => <li key={index}>{detail}</li>)}
            </Box>
            {onToggle && (
                <FormControlLabel
                    sx={{ mt: 0.5 }}
                    control={<Checkbox size="small" checked={acknowledged} onChange={onToggle} />}
                    label={<Typography sx={{ fontSize: 12.5, fontWeight: 600 }}>{acknowledgeLabel}</Typography>}
                />
            )}
        </Box>
    );
}

export default function TourProcessingFinalCommissionDialog({ open, check, subtitle, downloading, onClose, onConfirm }) {
    const [acknowledged, setAcknowledged] = useState(() => new Set());
    const blockers = check?.blockers ?? [];
    const blocked = blockers.length > 0;
    const pending = finalCommissionIssueKeys(check).filter((key) => !acknowledged.has(key));

    const toggle = (key) => () => setAcknowledged((current) => {
        const next = new Set(current);
        if (next.has(key)) next.delete(key);
        else next.add(key);
        return next;
    });

    return (
        <GeneralModal
            open={open}
            onClose={downloading ? undefined : onClose}
            icon={WarningAmberOutlinedIcon}
            title="Descargar Comisión Final"
            subtitle={subtitle}
            maxWidth="md"
            loading={downloading}
            secondaryButton={{ label: blocked ? 'Cerrar' : 'Cancelar', onClick: onClose, disabled: downloading }}
            primaryButton={blocked ? undefined : {
                label: downloading ? 'Generando...' : 'Descargar de todos modos',
                onClick: () => onConfirm({ rejectIncomplete: Boolean(check?.rejections) }),
                disabled: downloading || pending.length > 0,
            }}
        >
            <Stack spacing={2} sx={{ p: 3 }}>
                {blocked ? (
                    <Alert severity="error" variant="outlined">
                        No se puede descargar la Comisión Final. Cada viaje de estas giras debe tener chofer y vehículo, o ninguno de sus viajes debe tenerlos.
                    </Alert>
                ) : (
                    <Typography sx={{ fontSize: 13.5 }}>
                        Revisa lo siguiente antes de generar la Comisión Final. Confirma cada punto para habilitar la descarga.
                    </Typography>
                )}
                {blockers.map((issue) => (
                    <IssueCard key={issue.code} issue={issue} color="error" />
                ))}
                {!blocked && FINAL_COMMISSION_SECTIONS.map((section) => (check?.[section.key]?.length ? (
                    <Stack key={section.key} spacing={1.25}>
                        <Typography sx={{ fontWeight: 800, fontSize: 14 }}>{section.heading}</Typography>
                        {check[section.key].map((issue) => {
                            const key = `${section.key}:${issue.code}`;
                            return (
                                <IssueCard
                                    key={key}
                                    issue={issue}
                                    color={section.color}
                                    acknowledgeLabel={section.acknowledgeLabel}
                                    acknowledged={acknowledged.has(key)}
                                    onToggle={toggle(key)}
                                />
                            );
                        })}
                    </Stack>
                ) : null))}
                {!blocked && check?.rejections && (
                    <Stack spacing={1.25}>
                        <Typography sx={{ fontWeight: 800, fontSize: 14 }}>Giras que cambiarán a No aprobada</Typography>
                        <IssueCard
                            issue={check.rejections}
                            color="info"
                            acknowledgeLabel={REJECTIONS_LABEL}
                            acknowledged={acknowledged.has(REJECTIONS_KEY)}
                            onToggle={toggle(REJECTIONS_KEY)}
                        />
                    </Stack>
                )}
            </Stack>
        </GeneralModal>
    );
}
