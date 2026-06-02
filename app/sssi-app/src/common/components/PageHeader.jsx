import { Box, Typography } from '@mui/material';

export function PageHeader({
  title,
  description,
  action,
  sx,
  titleSx,
  descriptionSx,
}) {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: { xs: 'flex-start', md: 'center' },
        justifyContent: 'space-between',
        gap: 2,
        mb: 2,
        flexDirection: { xs: 'column', md: 'row' },
        ...sx,
      }}
    >
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Typography
          variant="h4"
          sx={{
            fontWeight: 800,
            color: 'primary.icon',
            fontSize: { xs: '1.55rem', sm: '1.9rem' },
            ...titleSx,
          }}
        >
          {title}
        </Typography>
        {description ? (
          <Typography
            sx={{
              color: 'text.secondary',
              fontSize: 14,
              ...descriptionSx,
            }}
          >
            {description}
          </Typography>
        ) : null}
      </Box>
      {action ? <Box sx={{ flexShrink: 0 }}>{action}</Box> : null}
    </Box>
  );
}


