import BusinessIcon from '@mui/icons-material/Business';
import PlaceIcon from '@mui/icons-material/Place';
import LocationBrowserPage from '../components/LocationBrowserPage.jsx';

export function BuildingPage() {
    return (
        <LocationBrowserPage
            mainEntity="building"
            pageTitle="Edificios"
            pageDescription="Administración de los edificios dentro de cada campus."
            cards={[
                { entityName: 'campus', title: 'Campus', icon: BusinessIcon, description: 'Campus universitario' },
                { entityName: 'location', title: 'Locaciones', icon: PlaceIcon, description: 'Espacio físico interno' },
            ]}
        />
    );
}

export default BuildingPage;
