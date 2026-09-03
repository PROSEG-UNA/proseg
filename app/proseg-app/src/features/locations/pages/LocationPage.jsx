import BusinessIcon from '@mui/icons-material/Business';
import ApartmentIcon from '@mui/icons-material/Apartment';
import LocationBrowserPage from '../components/LocationBrowserPage.jsx';

export function LocationPage() {
    return (
        <LocationBrowserPage
            mainEntity="location"
            pageTitle="Detalles de Ubicación"
            pageDescription="Administración de los espacios físicos internos de cada edificio."
            cards={[
                { entityName: 'campus', title: 'Campus', icon: BusinessIcon, description: 'Campus universitario' },
                { entityName: 'building', title: 'Edificios', icon: ApartmentIcon, description: 'Edificio dentro de una sede' },
            ]}
        />
    );
}

export default LocationPage;
