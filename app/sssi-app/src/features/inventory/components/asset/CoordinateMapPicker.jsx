import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png';
import markerIcon from 'leaflet/dist/images/marker-icon.png';
import markerShadow from 'leaflet/dist/images/marker-shadow.png';
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import { Box, TextField } from '@mui/material';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconUrl: markerIcon,
    iconRetinaUrl: markerIcon2x,
    shadowUrl: markerShadow,
});

const DEFAULT_CENTER = [9.9994159, -84.1116983];
const DEFAULT_ZOOM = 5;

function MapClickHandler({ onCoordinatesChange, disabled }) {
    useMapEvents({
        click(e) {
            if (!disabled) onCoordinatesChange(e.latlng.lat, e.latlng.lng);
        },
    });
    return null;
}

const fieldSx = { '& .MuiInputBase-input': { fontSize: '0.85rem' } };

export default function CoordinateMapPicker({
    latitude, longitude,
    onCoordinatesChange, onLatitudeChange, onLongitudeChange,
    onBlur, disabled, errors, touched,
}) {
    const lat = parseFloat(latitude);
    const lng = parseFloat(longitude);
    const hasCoords = !isNaN(lat) && !isNaN(lng);
    const center = hasCoords ? [lat, lng] : DEFAULT_CENTER;

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
            <Box sx={{ height: 300, borderRadius: 1, overflow: 'hidden', border: '1px solid', borderColor: 'divider', cursor: disabled ? 'default' : 'crosshair' }}>
                <MapContainer
                    center={center}
                    zoom={hasCoords ? 13 : DEFAULT_ZOOM}
                    style={{ height: '100%', width: '100%' }}
                    key={`${hasCoords}`}
                >
                    <TileLayer
                        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                    />
                    <MapClickHandler onCoordinatesChange={onCoordinatesChange} disabled={disabled} />
                    {hasCoords && <Marker position={[lat, lng]} />}
                </MapContainer>
            </Box>

            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
                <TextField
                    label="Latitud" value={latitude}
                    onChange={e => onLatitudeChange(e.target.value)}
                    onBlur={() => onBlur('latitude')}
                    fullWidth size="small" disabled={disabled}
                    type="number"
                    inputProps={{ step: 'any' }}
                    error={touched.latitude && !!errors.latitude}
                    helperText={touched.latitude ? (errors.latitude || ' ') : ' '}
                    placeholder="-33.4500000"
                    sx={fieldSx}
                />
                <TextField
                    label="Longitud" value={longitude}
                    onChange={e => onLongitudeChange(e.target.value)}
                    onBlur={() => onBlur('longitude')}
                    fullWidth size="small" disabled={disabled}
                    type="number"
                    inputProps={{ step: 'any' }}
                    error={touched.longitude && !!errors.longitude}
                    helperText={touched.longitude ? (errors.longitude || ' ') : ' '}
                    placeholder="-70.6500000"
                    sx={fieldSx}
                />
            </Box>
        </Box>
    );
}
