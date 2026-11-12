import { formatDate } from '../../transportUtils';
import { draftPeriodLabel } from '../tourProcessing/tourProcessingDraftLabels.js';

export const OPEN_DRAFT_TOURS_ACTION = {
    keep: 'KEEP',
    remove: 'REMOVE',
};

function tourLine(tour) {
    const period = tour?.year && tour?.month ? draftPeriodLabel(tour) : (tour?.fileName || 'Borrador');
    const number = tour?.number || 'sin número';
    const date = formatDate(tour?.departureDate);
    const when = date && date !== '—' ? `, ${date}` : '';
    return `${period}, gira ${number}${when}`;
}

function toursCountText(count) {
    return count === 1 ? '1 gira' : `${count} giras`;
}

export function formatVehicleUnavailablePrompt(plate, tours) {
    const count = tours.length;
    const assigned = count === 1 ? '1 gira asignada' : `${count} giras asignadas`;
    return [
        `El vehículo ${plate} tiene ${assigned} en borradores abiertos:`,
        tours.map(tourLine).join('\n'),
        '',
        'Eliminar apariciones: lo quita de esas giras.',
        'Mantener: queda en las giras con una advertencia.',
        'Cancelar: el vehículo sigue Disponible.',
    ].join('\n');
}

export function formatVehicleOpenDraftResult(action, count) {
    if (action === OPEN_DRAFT_TOURS_ACTION.remove) {
        return `Se eliminó el vehículo de ${toursCountText(count)} en borradores abiertos.`;
    }
    return `El vehículo se mantiene en ${toursCountText(count)} de borradores abiertos, con una advertencia.`;
}
