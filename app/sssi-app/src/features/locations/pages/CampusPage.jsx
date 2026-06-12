import ApartmentIcon from '@mui/icons-material/Apartment';
import PlaceIcon from '@mui/icons-material/Place';
import LocationBrowserPage from '../components/LocationBrowserPage.jsx';

export function CampusPage() {
    return (
        <LocationBrowserPage
            mainEntity="campus"
            pageTitle="Campus"
            pageDescription="Administración de los campus universitarios."
            cards={[
                { entityName: 'building', title: 'Edificios', icon: ApartmentIcon, description: 'Edificio dentro de una sede' },
                { entityName: 'location', title: 'Locaciones', icon: PlaceIcon, description: 'Espacio físico interno' },
            ]}
        />
    );
}

export default CampusPage;
