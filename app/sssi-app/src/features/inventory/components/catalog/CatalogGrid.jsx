import { Children, useState } from 'react';
import { Box, Button, Collapse, useTheme, useMediaQuery } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';

export default function CatalogGrid({ children }) {
    const theme = useTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
    const isLgUp = useMediaQuery(theme.breakpoints.up('lg'));

    const [view, setView] = useState('default');

    const all = Children.toArray(children);
    const firstRowCount = isMobile ? 1 : isLgUp ? 3 : 2;
    const firstRow = all.slice(0, firstRowCount);
    const rest = all.slice(firstRowCount);

    const accent = theme.vars.palette.tones.rose.fg;

    const nextView = () => setView(v =>
        v === 'default' ? 'expanded' : v === 'expanded' ? 'collapsed' : 'default'
    );

    const buttonLabel = view === 'expanded'
        ? 'Mostrar menos'
        : view === 'collapsed'
            ? `Mostrar ${all.length}`
            : `Mostrar ${rest.length} más`;

    const buttonIcon = view === 'expanded' ? <ExpandLessIcon /> : <ExpandMoreIcon />;

    const itemSx = {
        flex: '1 1 auto',
        minWidth: {
            xs: '100%',
            sm: 'calc(50% - 6px)',
            lg: 'calc(33.33% - 8px)',
        },
    };

    return (
        <Box>
            <Collapse in={view !== 'collapsed'} timeout={250}>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5 }}>
                    {firstRow.map((child, i) => (
                        <Box key={i} sx={itemSx}>{child}</Box>
                    ))}
                </Box>
            </Collapse>

            <Collapse in={view === 'expanded'} timeout={250}>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5, mt: 1.5 }}>
                    {rest.map((child, i) => (
                        <Box key={i} sx={itemSx}>{child}</Box>
                    ))}
                </Box>
            </Collapse>

            <Box sx={{ display: 'flex', justifyContent: 'center', mt: view === 'collapsed' ? -1.25 : 1.50 }}>
                <Button
                    onClick={nextView}
                    variant="text"
                    size="small"
                    endIcon={buttonIcon}
                    sx={{
                        color: accent,
                        fontWeight: 600,
                        textTransform: 'none',
                        fontSize: '0.78rem',
                        '&:hover': {
                            bgcolor: theme.vars.palette.tones.rose.softSubtle,
                        },
                    }}
                >
                    {buttonLabel}
                </Button>
            </Box>
        </Box>
    );
}
