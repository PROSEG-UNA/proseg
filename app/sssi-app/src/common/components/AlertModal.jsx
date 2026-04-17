import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import ErrorOutlinedIcon from '@mui/icons-material/ErrorOutlined';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';

const config = {
    success: { icon: CheckCircleOutlinedIcon, color: 'success.main', title: 'Éxito' },
    error:   { icon: ErrorOutlinedIcon,       color: 'error.main',   title: 'Error' },
    warning: { icon: WarningAmberIcon,        color: 'warning.main', title: 'Advertencia' },
    info:    { icon: InfoOutlinedIcon,        color: 'info.main',    title: 'Información' },
};

/**
 * Modal de alerta reutilizable.
 *
 * @param {'success'|'error'|'warning'|'info'} type
 * @param {string}   title    - Sobreescribe el título por defecto del tipo.
 * @param {string}   message  - Mensaje a mostrar.
 * @param {boolean}  open
 * @param {function} onClose
 */
export default function AlertModal({ type = 'info', title, message, open, onClose }) {
    const { icon: Icon, color, title: defaultTitle } = config[type] ?? config.info;

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="xs"
            fullWidth
            slotProps={{
                backdrop: { sx: { backdropFilter: 'blur(4px)' } },
            }}
        >
            <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Icon sx={{ color, fontSize: 28 }} />
                <Typography component="span" variant="h6" fontWeight={600}>
                    {title ?? defaultTitle}
                </Typography>
            </DialogTitle>

            <DialogContent>
                <Box sx={{ pt: 0.5 }}>
                    <Typography variant="body1">{message}</Typography>
                </Box>
            </DialogContent>

            <DialogActions>
                <Button variant="contained" onClick={onClose} autoFocus>
                    Aceptar
                </Button>
            </DialogActions>
        </Dialog>
    );
}
