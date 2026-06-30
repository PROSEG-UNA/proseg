import {
    Dialog, DialogContent,
    Box, Typography, Button, IconButton, LinearProgress,
    useTheme, useMediaQuery,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';

export default function GeneralModal({
    open,
    onClose,
    maxWidth = 'sm',
    fullScreenAt = 'sm',
    fillHeight = false,
    icon: Icon,
    title,
    subtitle,
    loading = false,
    showCloseButton = true,
    footerLeft,
    primaryButton,
    secondaryButton,
    contentSx,
    paperSx,
    children,
}) {
    const theme = useTheme();
    const isSmall = useMediaQuery(theme.breakpoints.down(fullScreenAt));
    const accentColor = theme.vars.palette.tones.rose.fg;
    const headerBg = theme.vars.palette.tones.rose.headerBg;
    const headerBgEnd = theme.vars.palette.tones.rose.headerBgEnd;
    const hoverBg = theme.vars.palette.tones.rose.hoverBg;
    const buttonShadow = theme.vars.palette.tones.rose.buttonShadow;
    const buttonShadowHover = theme.vars.palette.tones.rose.buttonShadowHover;
    const headerGradient = `linear-gradient(135deg, ${headerBg} 0%, ${headerBgEnd} 100%)`;

    return (
        <Dialog
            open={open}
            onClose={onClose}
            fullScreen={isSmall}
            maxWidth={maxWidth}
            fullWidth
            slotProps={{
                backdrop: { sx: { backdropFilter: 'blur(3px)' } },
                paper: {
                    sx: {
                        maxHeight: isSmall ? '100dvh' : '92dvh',
                        height: isSmall ? '100dvh' : (fillHeight ? '92dvh' : 'auto'),
                        borderRadius: isSmall ? 0 : '16px',
                        overflow: 'hidden',
                        display: 'flex',
                        flexDirection: 'column',
                        boxShadow: theme.vars.palette.tones.rose.shadowModal,
                        bgcolor: 'background.paperWarm',
                        ...paperSx,
                    },
                },
            }}
        >
            <Box
                sx={{
                    flexShrink: 0,
                    background: headerGradient,
                    px: { xs: 2.5, sm: 3 },
                    py: 2.25,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                }}
            >
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                    <Box
                        sx={{
                            width: 38, height: 38, borderRadius: '10px',
                            background: theme.vars.palette.tones.rose.headerOverlay,
                            border: `1px solid ${theme.vars.palette.tones.rose.headerBorder}`,
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                        }}
                    >
                        {Icon && <Icon sx={{ color: '#fff', fontSize: 20 }} />}
                    </Box>
                    <Box>
                        <Typography sx={{ color: '#fff', fontWeight: 800, fontSize: { xs: 15, sm: 15.5 }, lineHeight: 1.2, letterSpacing: '-0.01em' }}>
                            {title}
                        </Typography>
                        {subtitle && (
                            <Typography sx={{ color: theme.vars.palette.tones.rose.headerTextMuted, fontSize: { xs: 11, sm: 11.5 } }}>
                                {subtitle}
                            </Typography>
                        )}
                    </Box>
                </Box>
                {showCloseButton && (
                    <IconButton
                        onClick={onClose}
                        size="small"
                        sx={{
                            color: theme.vars.palette.tones.rose.headerTextSubtle,
                            border: `1px solid ${theme.vars.palette.tones.rose.headerTextBorder}`,
                            p: 0.625,
                            '&:hover': { bgcolor: theme.vars.palette.tones.rose.headerOverlayHover, color: '#fff' },
                        }}
                    >
                        <CloseIcon sx={{ fontSize: 16 }} />
                    </IconButton>
                )}
            </Box>

            {loading && (
                <LinearProgress
                    sx={{
                        flexShrink: 0, height: 2,
                        bgcolor: `color-mix(in srgb, ${accentColor} 15%, transparent)`,
                        '& .MuiLinearProgress-bar': { bgcolor: accentColor },
                    }}
                />
            )}

            <DialogContent sx={{ p: 0, flex: 1, minHeight: 0, bgcolor: 'background.paperWarm', ...contentSx }}>
                {children}
            </DialogContent>

            <Box
                sx={{
                    flexShrink: 0,
                    px: { xs: 2.5, sm: 3 }, py: 1.75,
                    borderTop: '1px solid', borderColor: 'divider',
                    display: 'flex', alignItems: 'center',
                    justifyContent: footerLeft ? 'space-between' : 'flex-end',
                    bgcolor: theme.vars.palette.tones.rose.footerBg,
                }}
            >
                {footerLeft}
                <Box sx={{ display: 'flex', gap: 1 }}>
                    {secondaryButton && (
                        <Button
                            onClick={secondaryButton.onClick}
                            variant="outlined"
                            size="small"
                            disabled={secondaryButton.disabled}
                            sx={{
                                borderColor: 'divider', color: 'text.secondary',
                                textTransform: 'none', fontWeight: 600, fontSize: 12.5, borderRadius: '8px',
                                '&:hover': { borderColor: theme.vars.palette.tones.rose.secondaryHoverBg, color: accentColor, bgcolor: theme.vars.palette.tones.rose.secondaryHoverBg },
                            }}
                        >
                            {secondaryButton.label}
                        </Button>
                    )}
                    {primaryButton && (
                        <Button
                            onClick={primaryButton.onClick}
                            variant={primaryButton.variant ?? "outlined"}
                            size="small"
                            startIcon={primaryButton.startIcon}
                            disabled={primaryButton.disabled}
                            loading={primaryButton.loading}
                            sx={{
                                borderColor: 'divider', color: 'text.secondary',
                                textTransform: 'none', fontWeight: 600, fontSize: 12.5, borderRadius: '8px',
                                '&:hover': { borderColor: 'text.disabled', color: 'text.primary', bgcolor: 'hsla(220, 20%, 50%, 0.08)' },
                                '&:disabled': { opacity: 0.55 },
                                ...primaryButton.sx,
                            }}
                        >
                            {primaryButton.label}
                        </Button>
                    )}
                </Box>
            </Box>
        </Dialog>
    );
}