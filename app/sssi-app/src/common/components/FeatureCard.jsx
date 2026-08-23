import { Box, Button, Typography } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';

export function FeatureCard({ icon, title, description, buttonLabel, onNavigate, fullHeight = false, sx: customSx }) {
    const theme = useTheme();
    const t = theme.palette.tones.rose;

    return (
        <Box
            sx={(th) => ({
                position: 'relative',
                overflow: 'hidden',
                display: 'flex',
                flexDirection: 'column',
                gap: 2,
                p: 2.5,
                // ensure consistent sizing inside Grid
                flex: fullHeight ? 1 : undefined,
                height: fullHeight ? '100%' : undefined,
                minHeight: { md: 220 },
                flexGrow: { xs: 0, md: 1 },
                flexShrink: { xs: 0, md: 1 },
                flexBasis: { xs: '100%', md: 0 },
                maxWidth: { xs: '100%', lg: '340px' },
                borderRadius: '16px',
                border: '1px solid',
                borderColor: 'divider',
                background: `linear-gradient(180deg, color-mix(in srgb, ${th.vars.palette.background.paperWarm} 95%, transparent) 0%, color-mix(in srgb, ${th.vars.palette.background.paperWarm} 78%, transparent) 100%)`,
                backdropFilter: 'blur(8px)',
                WebkitBackdropFilter: 'blur(8px)',
                cursor: 'pointer',
                transition: 'transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease',
                boxShadow: t.shadowResting,
                '&::before': {
                    content: '""',
                    position: 'absolute',
                    inset: 0,
                    background: `radial-gradient(circle at 100% 0%, ${t.glow} 0%, transparent 55%)`,
                    opacity: 0.9,
                    pointerEvents: 'none',
                },
                '&:hover': {
                    transform: 'translateY(-3px)',
                    borderColor: t.ring,
                    boxShadow: t.shadowHover,
                },
                '&:hover .fc-icon': {
                    borderColor: t.ring,
                    transform: 'rotate(-4deg) scale(1.06)',
                },
                ...(typeof customSx === 'function' ? customSx(th) : (customSx || {})),
            })}
            onClick={onNavigate}
        >
            <Box sx={{ position: 'relative', zIndex: 1 }}>
                <Box
                    className="fc-icon"
                    sx={{
                        width: 48, height: 48,
                        borderRadius: '13px',
                        background: t.soft,
                        border: `1px solid ${t.ring}`,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        transition: 'transform 0.22s ease, border-color 0.22s ease',
                        mb: 2,
                        fontSize: 24,
                        color: t.fg,
                        '& .MuiSvgIcon-root': { fontSize: 'inherit', color: 'inherit' },
                    }}
                >
                    {icon}
                </Box>

                <Typography sx={{ fontSize: '1rem', fontWeight: 700, color: 'text.primary', letterSpacing: '-0.01em', lineHeight: 1.3, mb: 0.75 }}>
                    {title}
                </Typography>
                {description && (
                    <Typography sx={{ fontSize: '0.82rem', color: 'text.secondary', lineHeight: 1.5 }}>
                        {description}
                    </Typography>
                )}
            </Box>

            <Box
                sx={{ position: 'relative', zIndex: 1, mt: 'auto', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}
                onClick={(e) => { e.stopPropagation(); onNavigate?.(); }}
            >
                <Button
                    size="small"
                    variant="contained"
                    disableElevation
                    endIcon={<ArrowForwardIcon sx={{ fontSize: '14px !important' }} />}
                    sx={(th) => ({
                        textTransform: 'none',
                        fontWeight: 700,
                        fontSize: '0.8rem',
                        borderRadius: '9px',
                        px: 2,
                        py: 0.75,
                        color: '#fff',
                        background: `linear-gradient(135deg, ${th.vars.palette.tones.rose.headerBg} 0%, ${th.vars.palette.tones.rose.headerBg} 100%)`,
                        boxShadow: t.shadowResting,
                        transition: 'box-shadow 0.22s ease, transform 0.22s ease, background 0.22s ease',
                        '&:hover': {
                            boxShadow: t.shadowHover,
                            transform: 'translateX(2px)',
                        },
                    })}
                >
                    {buttonLabel}
                </Button>
            </Box>
        </Box>
    );
}

export default FeatureCard;
