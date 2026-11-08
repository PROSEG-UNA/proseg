import { Box, Typography } from '@mui/material';
import { formatHours } from '../../transportUtils.js';

export default function DriverSurplusSummary({ surplusCapPerDriver, driver }) {
    const compliance = driver && surplusCapPerDriver > 0
        ? driver.surplusHours / surplusCapPerDriver
        : null;

    return (
        <Box
            component="dl"
            sx={{
                display: 'flex',
                alignItems: 'center',
                flexWrap: 'wrap',
                columnGap: 3,
                rowGap: 1.5,
                m: 0,
                '& dt': { fontSize: 12, lineHeight: 1.4, color: 'text.secondary' },
                '& dd': { m: 0, fontSize: 14, lineHeight: 1.5, fontWeight: 600, fontVariantNumeric: 'tabular-nums', color: 'text.primary' },
            }}
        >
            <Box>
                <Typography component="dt">Horas sobresueldo del mes</Typography>
                <Typography component="dd">{`${formatHours(surplusCapPerDriver)} h${driver ? '' : ' por chofer'}`}</Typography>
            </Box>
            {driver && (
                <Box>
                    <Typography component="dt">{`Sobresueldo de ${driver.name}`}</Typography>
                    <Typography component="dd">{`${formatHours(driver.surplusHours)} h`}</Typography>
                </Box>
            )}
            {compliance !== null && (
                <Box>
                    <Typography component="dt">Cumplimiento</Typography>
                    <Typography component="dd">{`${(compliance * 100).toFixed(1)}%`}</Typography>
                </Box>
            )}
        </Box>
    );
}
