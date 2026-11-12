import { Box, Typography } from '@mui/material';
import { formatHours } from '../../transportUtils.js';

export default function DriverSurplusSummary({ surplusCapPerDriver, driver }) {
    const surplusCap = driver?.surplusCap ?? surplusCapPerDriver;
    const compliance = driver && surplusCap > 0
        ? driver.surplusHours / surplusCap
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
                <Typography component="dd">{`${formatHours(surplusCap)} h${driver ? '' : ' por defecto'}`}</Typography>
            </Box>
            {driver?.businessDays != null && (
                <Box>
                    <Typography component="dt">Días hábiles</Typography>
                    <Typography component="dd">{driver.businessDays}</Typography>
                </Box>
            )}
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
