import { formatDate } from '../../transportUtils';
import { draftPeriodLabel } from '../tourProcessing/tourProcessingDraftLabels.js';

export function formatVehicleUnavailableWarning(tours) {
    const count = tours.length;
    const assignment = count === 1 ? '1 gira asignada' : `${count} giras asignadas`;
    const lines = tours.map((tour) => {
        const period = tour?.year && tour?.month ? draftPeriodLabel(tour) : (tour?.fileName || 'Borrador');
        const number = tour?.number || 'sin número';
        const date = formatDate(tour?.departureDate);
        const when = date && date !== '—' ? `, ${date}` : '';
        return `${period}, gira ${number}${when}`;
    });
    return `El vehículo quedó como No disponible y tiene ${assignment} en borradores abiertos. Cámbialas a mano:\n${lines.join('\n')}`;
}
