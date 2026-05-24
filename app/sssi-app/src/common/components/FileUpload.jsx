import { useState } from 'react';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import {
  Alert,
  Box,
  Button,
  Chip,
  LinearProgress,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { uploadArchiveFile } from '../services/archiveService';

export function FileUpload({
  title = 'Subir archivo',
  description = 'Selecciona un archivo para subir al servidor.',
  accept,
  maxSizeMB = 25,
  folder,
  objectName,
  autoUpload = false,
  disabled = false,
  onSuccess,
  onError,
  uploadFn = uploadArchiveFile,
}) {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleSelectFile = (event) => {
    const nextFile = event.target.files && event.target.files[0];
    if (!nextFile) return;

    if (maxSizeMB && nextFile.size > maxSizeMB * 1024 * 1024) {
      setError(`El archivo supera ${maxSizeMB} MB.`);
      setSuccess('');
      setFile(null);
      return;
    }

    setFile(nextFile);
    setError('');
    setSuccess('');

    if (autoUpload) {
      handleUpload(nextFile);
    }
  };

  const handleUpload = async (selectedFile = file) => {
    if (!selectedFile || uploading) return;

    setUploading(true);
    setProgress(0);
    setError('');
    setSuccess('');

    try {
      const response = await uploadFn({
        file: selectedFile,
        folder,
        objectName,
        onProgress: setProgress,
      });
      setSuccess(response?.message || 'Archivo subido correctamente.');
      if (onSuccess) onSuccess(response);
    } catch (err) {
      const message =
        err?.response?.data?.message || err?.message || 'Error al subir archivo.';
      setError(message);
      if (onError) onError(err);
    } finally {
      setUploading(false);
    }
  };

  const handleClear = () => {
    setFile(null);
    setError('');
    setSuccess('');
    setProgress(0);
  };

  const fileLabel = file ? `${file.name} (${Math.round(file.size / 1024)} KB)` : 'Sin archivo';

  return (
    <Paper
      elevation={0}
      sx={{
        p: 3,
        borderRadius: 2,
        border: '1px solid',
        borderColor: 'divider',
        backgroundColor: 'background.paper',
      }}
    >
      <Stack spacing={2}>
        <Box>
          <Typography variant="h6" fontWeight={600}>
            {title}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {description}
          </Typography>
        </Box>

        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
          <TextField
            type="file"
            size="small"
            fullWidth
            disabled={disabled || uploading}
            onChange={handleSelectFile}
            inputProps={{ accept }}
            sx={{ maxWidth: { xs: '100%', sm: 420 } }}
          />

          <Button
            variant="text"
            color="inherit"
            startIcon={<DeleteOutlineOutlinedIcon />}
            disabled={disabled || uploading || !file}
            onClick={handleClear}
          >
            Limpiar
          </Button>
        </Stack>

        <Chip
          label={fileLabel}
          variant="outlined"
          sx={{ width: '100%' }}
        />

        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
          <Button
            variant="contained"
            startIcon={<CloudUploadOutlinedIcon />}
            onClick={() => handleUpload()}
            disabled={disabled || uploading || !file}
          >
            Subir
          </Button>

          {uploading && (
            <Box sx={{ flex: 1, width: '100%' }}>
              <LinearProgress variant="determinate" value={progress} />
              <Typography variant="caption" color="text.secondary">
                {progress}%
              </Typography>
            </Box>
          )}
        </Stack>

        {error && <Alert severity="error">{error}</Alert>}
        {success && <Alert severity="success">{success}</Alert>}
      </Stack>
    </Paper>
  );
}
