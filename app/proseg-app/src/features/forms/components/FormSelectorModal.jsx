import { Box, Dialog, DialogContent, DialogTitle, List, ListItemButton, ListItemText, Typography } from '@mui/material';
import AssignmentIcon from '@mui/icons-material/Assignment';
import { useFormTypes } from '../hooks/useFormTypes';

export default function FormSelectorModal({ open, onClose, onSelect }) {
    const { formTypes, loading, error } = useFormTypes();

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
            <DialogTitle>Seleccionar tipo de formulario</DialogTitle>
            <DialogContent dividers>
                {loading ? (
                    <Typography>Cargando tipos de formulario...</Typography>
                ) : error ? (
                    <Typography color="error">{error}</Typography>
                ) : (
                    <List sx={{ p: 0 }}>
                        {formTypes.map((type) => (
                            <ListItemButton
                                key={type.id}
                                onClick={() => onSelect(type)}
                                sx={{ borderRadius: 2, mb: 1, border: '1px solid', borderColor: 'divider' }}
                            >
                                <Box sx={{ mr: 1.5, display: 'flex', alignItems: 'center' }}>
                                    <AssignmentIcon color="primary" />
                                </Box>
                                <ListItemText
                                    primary={type.name}
                                    secondary={type.description || type.code}
                                />
                            </ListItemButton>
                        ))}
                    </List>
                )}
            </DialogContent>
        </Dialog>
    );
}
