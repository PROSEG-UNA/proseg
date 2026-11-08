import { Box, InputAdornment, TextField, Typography, useTheme } from '@mui/material';

export function AssetSectionTitle({ children, icon: Icon }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    return (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, mb: 1.5 }}>
            {Icon && <Icon sx={{ fontSize: 16, color: accentColor }} />}
            <Typography sx={{
                fontSize: 11.5,
                fontWeight: 700,
                letterSpacing: '0.08em',
                textTransform: 'uppercase',
                color: accentColor,
            }}>
                {children}
            </Typography>
        </Box>
    );
}

export function AssetReadOnlyField({ label, value, icon: Icon, multiline = false, minRows }) {
    const displayValue = value === null || value === undefined || value === '' ? '—' : value;

    return (
        <TextField
            label={label}
            value={displayValue}
            fullWidth
            size="small"
            multiline={multiline}
            minRows={minRows}
            slotProps={{
                input: {
                    readOnly: true,
                    startAdornment: Icon ? (
                        <InputAdornment position="start">
                            <Icon sx={{ fontSize: 17, color: 'text.secondary' }} />
                        </InputAdornment>
                    ) : undefined,
                },
            }}
            sx={{
                '& .MuiOutlinedInput-root': {
                    borderRadius: '10px',
                    '& fieldset': { borderColor: 'divider' },
                },
                '& .MuiInputBase-input': {
                    color: 'text.primary',
                    overflowWrap: 'anywhere',
                },
            }}
        />
    );
}
