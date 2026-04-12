import {
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Box,
  Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import TuneIcon from '@mui/icons-material/Tune';
import AppTheme from '../theme/AppTheme';

export function FilterAccordion({ children, title = 'Filtros' }) {
  return (
    <AppTheme>
      <Accordion sx={{ flex: 1, width: '100%', minWidth: 0 }}>
        <AccordionSummary
          expandIcon={<ExpandMoreIcon />}
          aria-controls="filter-content"
          id="filter-header"
          sx={{
            backgroundColor: 'background.paper',
            border: '1px solid',
            borderColor: 'divider',
            borderRadius: '4px',
            '&:hover': {
              backgroundColor: 'action.hover',
            },
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <TuneIcon sx={{ color: 'primary.main' }} />
            <Typography sx={{ fontWeight: 600, color: 'text.primary' }}>
              {title}
            </Typography>
          </Box>
        </AccordionSummary>
        <AccordionDetails
          sx={{
            backgroundColor: 'background.default',
            borderLeft: '1px solid',
            borderRight: '1px solid',
            borderBottom: '1px solid',
            borderColor: 'divider',
            borderRadius: '0 0 4px 4px',
            display: 'flex',
            flexDirection: 'column',
            gap: 2,
            p: 2,
            width: '100%',
          }}
        >
          {children}
        </AccordionDetails>
      </Accordion>
    </AppTheme>
  );
}

export default FilterAccordion;