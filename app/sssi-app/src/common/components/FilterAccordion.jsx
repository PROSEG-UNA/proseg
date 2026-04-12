import {
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Box,
  Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import TuneIcon from '@mui/icons-material/Tune';

export function FilterAccordion({ children, title = 'Filtros' }) {
  return (
    <Accordion sx={{ flex: 1, width: '100%', minWidth: 0 }}>
      <AccordionSummary
        expandIcon={<ExpandMoreIcon />}
        aria-controls="filter-content"
        id="filter-header"
        sx={{
          backgroundColor: '#f5f5f5',
          border: '1px solid #e0e0e0',
          borderRadius: '4px',
          '&:hover': {
            backgroundColor: '#efefef',
          },
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <TuneIcon sx={{ color: '#C41E3A' }} />
          <Typography sx={{ fontWeight: 600, color: '#333' }}>
            {title}
          </Typography>
        </Box>
      </AccordionSummary>
      <AccordionDetails
        sx={{
          backgroundColor: '#fafafa',
          borderLeft: '1px solid #e0e0e0',
          borderRight: '1px solid #e0e0e0',
          borderBottom: '1px solid #e0e0e0',
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
  );
}

export default FilterAccordion;
