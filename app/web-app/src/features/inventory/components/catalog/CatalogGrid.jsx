import { Children, useState } from 'react';
import { Box, Button, Collapse, useTheme, useMediaQuery } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';

export default function CatalogGrid({ children }) {
    const theme = useTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

    const [expanded, setExpanded] = useState(false);

    const all = Children.toArray(children);
    const firstRowCount = isMobile ? 1 : 2;
    const firstRow = all.slice(0, firstRowCount);
    const rest = all.slice(firstRowCount);
    const hasMore = rest.length > 0;

    const accent = theme.vars.palette.tones.rose.fg;

    const itemSx = (i) => ({
        flex: '1 1 auto',
        minWidth: {
            xs: '100%',
            sm: 'calc(50% - 6px)',
            md: i < 2 ? 'calc(50% - 6px)' : 'calc(33.33% - 8px)',
        },
    });

    return (
        <Box>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5 }}>
                {firstRow.map((child, i) => (
                    <Box key={i} sx={itemSx(i)}>{child}</Box>
                ))}
            </Box>

            {hasMore && (
                <Collapse in={expanded} timeout={250}>
                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5, mt: 1.5 }}>
                        {rest.map((child, i) => (
                            <Box key={i} sx={itemSx(firstRowCount + i)}>{child}</Box>
                        ))}
                    </Box>
                </Collapse>
            )}

            {hasMore && (
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 1.25 }}>
                    <Button
                        onClick={() => setExpanded(v => !v)}
                        variant="text"
                        size="small"
                        endIcon={expanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
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
                        {expanded ? 'Mostrar menos' : `Mostrar ${rest.length} más`}
                    </Button>
                </Box>
            )}
        </Box>
    );
}
