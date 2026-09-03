import { Box, Typography } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';

export default function CatalogCard({ title, entityName, onClick, icon: Icon, tone = 'rose', description }) {
    const theme = useTheme();
    const t = theme.palette.tones[tone] ?? theme.palette.tones.rose;

    return (
        <Box
            onClick={() => onClick(entityName)}
            sx={{
                position: 'relative',
                overflow: 'hidden',
                display: 'flex',
                alignItems: 'center',
                gap: 1.5,
                p: 1.5,
                minHeight: { xs: 'unset', md: 95 },
                borderRadius: '14px',
                border: '1px solid',
                borderColor: 'divider',
                background: `linear-gradient(180deg, color-mix(in srgb, ${theme.vars.palette.background.paperWarm} 95%, transparent) 0%, color-mix(in srgb, ${theme.vars.palette.background.paperWarm} 78%, transparent) 100%)`,
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
                    transform: 'translateY(-2px)',
                    borderColor: t.ring,
                    boxShadow: t.shadowHover,
                },
                '&:hover .cc-cta': { color: t.fg, transform: 'translateX(3px)' },
                '&:hover .cc-icon': { transform: 'rotate(-4deg) scale(1.05)' },
            }}
        >
            {Icon && (
                <Box
                    className="cc-icon"
                    sx={{
                        width: 36, height: 36, flexShrink: 0,
                        borderRadius: '10px',
                        background: t.soft,
                        border: `1px solid ${t.ring}`,
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        transition: 'transform 0.22s ease',
                        position: 'relative', zIndex: 1,
                    }}
                >
                    <Icon sx={{ fontSize: 18, color: t.fg }} />
                </Box>
            )}

            <Box sx={{ flex: 1, minWidth: 0, position: 'relative', zIndex: 1 }}>
                <Typography sx={{ fontSize: '0.875rem', fontWeight: 700, color: 'text.primary', letterSpacing: '-0.01em', lineHeight: 1.2 }}>
                    {title}
                </Typography>
                {description && (
                    <Typography sx={{ fontSize: '0.72rem', color: 'text.secondary', mt: 0.2, lineHeight: 1.3 }}>
                        {description}
                    </Typography>
                )}
            </Box>

            <Box
                className="cc-cta"
                sx={{
                    flexShrink: 0,
                    display: 'flex', alignItems: 'center',
                    color: 'text.disabled',
                    transition: 'color 0.2s ease, transform 0.2s ease',
                    position: 'relative', zIndex: 1,
                }}
            >
                <ArrowForwardIcon sx={{ fontSize: 15 }} />
            </Box>
        </Box>
    );
}
