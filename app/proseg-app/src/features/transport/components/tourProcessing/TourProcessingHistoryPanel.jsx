import { Typography } from '@mui/material';
import TourProcessingDraftsTable from './TourProcessingDraftsTable.jsx';

export default function TourProcessingHistoryPanel() {
    return (
        <>
            <Typography sx={{ fontWeight: 700, fontSize: 18, mb: 2 }}>Depuraciones terminadas</Typography>
            <TourProcessingDraftsTable status="CLOSED" />
        </>
    );
}
