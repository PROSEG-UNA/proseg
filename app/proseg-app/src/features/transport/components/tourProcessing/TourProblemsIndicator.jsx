import { Box, IconButton, Tooltip, Typography } from '@mui/material';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';

export default function TourProblemsIndicator({ problems }) {
    if (!problems?.length) return null;

    const tooltip = (
        <Box sx={{ maxWidth: 320, p: 1.25 }}>
            {problems.map((problem, index) => (
                <Box key={`${problem.title}-${index}`} sx={{ mt: index === 0 ? 0 : 1 }}>
                    <Typography variant="caption" sx={{ display: 'block', fontWeight: 700, color: 'text.primary' }}>
                        {problem.title}
                    </Typography>
                    <Typography variant="caption" sx={{ display: 'block', color: 'text.secondary', lineHeight: 1.4 }}>
                        {problem.detail}
                    </Typography>
                </Box>
            ))}
        </Box>
    );

    return (
        <Tooltip
            title={tooltip}
            placement="bottom-start"
            slotProps={{
                tooltip: {
                    sx: {
                        maxWidth: 'none',
                        p: 0,
                        bgcolor: 'background.paper',
                        color: 'text.primary',
                        border: '1px solid',
                        borderColor: 'divider',
                        borderRadius: 2,
                        boxShadow: 3,
                    },
                },
            }}
        >
            <IconButton
                type="button"
                size="small"
                aria-label={`Problemas de la gira: ${problems.map((problem) => problem.title).join(', ')}`}
                onClick={(event) => event.stopPropagation()}
                sx={{ color: 'error.main' }}
            >
                <ErrorOutlineOutlinedIcon fontSize="small" />
            </IconButton>
        </Tooltip>
    );
}
