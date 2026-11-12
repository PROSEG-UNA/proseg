import { Alert, Box, Button, Chip, Skeleton, Typography } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import SellOutlinedIcon from '@mui/icons-material/SellOutlined';

const SKELETON_WIDTHS = [88, 120, 72, 104, 96];

export default function KeywordChipWell({ ariaLabel, rows, loading, error, onRetry, canEdit, canDelete, onEdit, onDelete, loadingLabel, emptyTitle, emptyHint }) {
    return (
        <Box
            role="group"
            aria-label={ariaLabel}
            aria-busy={loading || undefined}
            sx={{
                minHeight: 96,
                maxHeight: 248,
                overflowY: 'auto',
                p: 2,
                borderRadius: 2,
                bgcolor: 'background.paper',
                border: '1px solid',
                borderColor: 'divider',
                display: 'flex',
                alignContent: 'flex-start',
                alignItems: 'flex-start',
                flexWrap: 'wrap',
                gap: 1,
                scrollbarWidth: 'thin',
            }}
        >
            {loading ? (
                <Box role="status" aria-label={loadingLabel} sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, width: '100%' }}>
                    {SKELETON_WIDTHS.map((width) => (
                        <Skeleton key={width} variant="rounded" width={width} height={32} sx={{ borderRadius: '8px' }} />
                    ))}
                </Box>
            ) : error ? (
                <Alert severity="error" sx={{ width: '100%' }} action={<Button color="inherit" size="small" onClick={onRetry}>Reintentar</Button>}>
                    {error}
                </Alert>
            ) : rows.length === 0 ? (
                <Box sx={{ width: '100%', minHeight: 80, display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 0.5, textAlign: 'center' }}>
                    <SellOutlinedIcon sx={{ fontSize: 22, color: 'text.disabled' }} />
                    <Typography sx={{ fontWeight: 600, fontSize: 14, color: 'text.primary' }}>{emptyTitle}</Typography>
                    <Typography sx={{ fontSize: 13, color: 'text.secondary' }}>{emptyHint}</Typography>
                </Box>
            ) : rows.map((keyword) => (
                <Chip
                    key={keyword.id}
                    label={keyword.text}
                    onClick={canEdit ? () => onEdit(keyword) : undefined}
                    onDelete={canDelete ? () => onDelete(keyword) : undefined}
                    deleteIcon={canDelete ? <CloseIcon aria-label={`Eliminar ${keyword.text}`} /> : undefined}
                    sx={{
                        maxWidth: '100%',
                        height: 32,
                        borderRadius: '8px',
                        fontWeight: 600,
                        fontSize: 13.5,
                        bgcolor: 'background.paperWarm',
                        color: 'text.primary',
                        border: '1px solid',
                        borderColor: 'divider',
                        transition: 'border-color 160ms ease-out, background-color 160ms ease-out',
                        '&:hover, &:focus-visible': { bgcolor: 'background.paperWarm', borderColor: 'primary.main' },
                        '& .MuiChip-deleteIcon': { fontSize: 16, color: 'text.secondary', transition: 'color 160ms ease-out' },
                        '& .MuiChip-deleteIcon:hover': { color: 'error.main' },
                    }}
                />
            ))}
        </Box>
    );
}
