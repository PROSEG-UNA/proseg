export const fieldSx = (theme) => ({
    '& .MuiOutlinedInput-root': {
        '& fieldset': { borderColor: theme.palette.grey[400] },
        '&:hover fieldset': { borderColor: theme.palette.primary.main },
    },
});
