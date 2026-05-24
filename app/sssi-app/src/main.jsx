import {StrictMode} from 'react';
import {createRoot} from 'react-dom/client';
import './index.css';
import App from './App.jsx';
import {HelmetProvider} from "react-helmet-async";
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <HelmetProvider>
            <LocalizationProvider dateAdapter={AdapterDayjs}>
                <App/>
            </LocalizationProvider>
        </HelmetProvider>
    </StrictMode>
);
