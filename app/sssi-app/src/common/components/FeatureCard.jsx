import { Box, Button, Card, CardActions, CardContent, Typography } from '@mui/material';
import { alpha } from '@mui/material/styles';

export function FeatureCard({ icon, title, description, buttonLabel, onNavigate }) {
  return (
    <Card
      sx={(theme) => ({
        width: { xs: '100%', sm: '45%', md: '400px' },
        display: 'flex',
        flexDirection: 'column',
        transition: 'box-shadow 0.3s ease',
        '&:hover': {
          boxShadow: `0 8px 16px ${alpha(theme.palette.primary.main, 0.15)}`,
        },
      })}
    >
      <CardContent sx={{ flex: 1, textAlign: 'center' }}>
        <Box sx={{ fontSize: { xs: 50, sm: 60 }, color: 'primary.main', mb: 2 }}>
          {icon}
        </Box>
        <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 1, color: 'text.primary' }}>
          {title}
        </Typography>
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {description}
        </Typography>
      </CardContent>
      <CardActions sx={{ justifyContent: 'center', pb: 2 }}>
        <Button
          variant="contained"
          onClick={onNavigate}
          sx={{
            backgroundColor: 'primary.main',
            '&:hover': { backgroundColor: 'primary.dark' },
            textTransform: 'none',
            fontWeight: 600,
          }}
        >
          {buttonLabel}
        </Button>
      </CardActions>
    </Card>
  );
}

export default FeatureCard;
