import { Box, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { PrimaryButton } from '../../../../common/components/PrimaryButton.jsx';

export default function SettingsSectionHeader({ titleId, component = 'h2', title, count, description, onAdd, addLabel = 'Agregar', sx }) {
    const isPrimary = component === 'h2';

    return (
        <Box sx={sx}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', columnGap: 2, rowGap: 1.5 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, minWidth: 0, flex: '1 1 260px' }}>
                    <Typography
                        id={titleId}
                        component={component}
                        sx={{ fontWeight: 700, fontSize: isPrimary ? '1.2rem' : '1.05rem', lineHeight: 1.3, textWrap: 'balance' }}
                    >
                        {title}
                    </Typography>
                    {typeof count === 'number' ? (
                        <Box
                            component="span"
                            aria-label={`${count} en total`}
                            sx={{
                                minWidth: 26,
                                height: 22,
                                px: 0.875,
                                display: 'inline-flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                flexShrink: 0,
                                borderRadius: '11px',
                                bgcolor: 'background.paper',
                                border: '1px solid',
                                borderColor: 'divider',
                                color: 'text.secondary',
                                fontSize: 12,
                                fontWeight: 700,
                                fontVariantNumeric: 'tabular-nums',
                            }}
                        >
                            {count}
                        </Box>
                    ) : null}
                </Box>
                {onAdd ? (
                    <PrimaryButton startIcon={<AddIcon />} onClick={onAdd} sx={{ px: 2.5, py: 0.75, flexShrink: 0 }}>
                        {addLabel}
                    </PrimaryButton>
                ) : null}
            </Box>
            {description ? (
                <Typography sx={{ mt: 0.75, color: 'text.secondary', fontSize: 14, lineHeight: 1.6, maxWidth: '68ch' }}>
                    {description}
                </Typography>
            ) : null}
        </Box>
    );
}
