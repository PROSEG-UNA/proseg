import { alpha } from '@mui/material/styles';
import { brand } from '../themePrimitives';

export const treeViewCustomizations = {
  MuiTreeItem2: {
    styleOverrides: {
      root: ({ theme }) => ({
        '--TreeView-itemChildrenIndentation': '12px',
        '& .MuiTreeItem2-content': {
          borderRadius: theme.shape.borderRadius,
          padding: '4px 8px',
          '&:hover': { backgroundColor: (theme.vars || theme).palette.action.hover },
          '&.Mui-selected': {
            backgroundColor: alpha(brand[300], 0.2),
            '&:hover': { backgroundColor: alpha(brand[300], 0.3) },
            '& .MuiTreeItem2-label': { fontWeight: 500 },
          },
          '&.Mui-focused': {
            outline: `3px solid ${alpha(brand[500], 0.5)}`,
            outlineOffset: '-2px',
          },
          ...theme.applyStyles('dark', {
            '&.Mui-selected': {
              backgroundColor: alpha(brand[600], 0.3),
              '&:hover': { backgroundColor: alpha(brand[600], 0.4) },
            },
          }),
        },
      }),
    },
  },
};
