import { act, cleanup, render } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import DriversTable from '../DriversTable.jsx';

const tableProps = vi.hoisted(() => ({ latest: null }));
const driversRequest = vi.hoisted(() => ({ latest: null }));

vi.mock('../../../../../common/components/TablaBase.jsx', () => ({
    default: (props) => {
        tableProps.latest = props;
        return null;
    },
}));

vi.mock('../../../../../common/hooks/usePermissions', () => ({
    usePermissions: () => ({ hasPermission: () => false }),
}));

vi.mock('../../../hooks/useTransportDriversData', () => ({
    useTransportDriversData: (request) => {
        driversRequest.latest = request;
        return { rows: [], loading: false, fetching: false, error: null, totalElements: 0 };
    },
}));

const monthData = { enabled: true, year: 2026, month: 5, loading: false, byDriverId: new Map() };

const sortBy = (id, desc = false) => act(() => tableProps.latest.tableOptions.onSortingChange([{ id, desc }]));

beforeEach(() => {
    localStorage.clear();
    tableProps.latest = null;
    driversRequest.latest = null;
});

afterEach(cleanup);

describe('orden por columnas del mes', () => {
    it('manda el orden de una columna del mes junto con el año y el mes', () => {
        render(<DriversTable monthData={monthData} unit="GENERAL_SERVICES" />);

        sortBy('surplusHoursAccrued', true);

        expect(driversRequest.latest.sort).toEqual(['surplusHoursAccrued,desc']);
        expect(driversRequest.latest.filters).toMatchObject({ year: 2026, month: 5, organizationalUnit: 'GENERAL_SERVICES' });
    });

    it('manda el orden del porcentaje de sobresueldo', () => {
        render(<DriversTable monthData={monthData} unit="GENERAL_SERVICES" />);

        sortBy('surplusExecutionRatio');

        expect(driversRequest.latest.sort).toEqual(['surplusExecutionRatio,asc']);
    });

    it('no manda año ni mes cuando se ordena por una columna que no es del mes', () => {
        render(<DriversTable monthData={monthData} unit="GENERAL_SERVICES" />);

        sortBy('name');

        expect(driversRequest.latest.sort).toEqual(['name,asc']);
        expect(driversRequest.latest.filters).not.toHaveProperty('year');
        expect(driversRequest.latest.filters).not.toHaveProperty('month');
    });

    it('descarta el orden de una columna del mes cuando no hay mes seleccionado', () => {
        render(<DriversTable monthData={{ enabled: false, byDriverId: new Map() }} unit="GENERAL_SERVICES" />);

        sortBy('businessDays');

        expect(driversRequest.latest.sort).toEqual([]);
        expect(driversRequest.latest.filters).not.toHaveProperty('year');
    });
});

describe('orden y filtros de las columnas de la tabla', () => {
    it('manda el filtro del estado y el orden del nombre', () => {
        render(<DriversTable monthData={null} unit="OTHER_UNIT" />);

        act(() => tableProps.latest.tableOptions.onColumnFiltersChange([{ id: 'status', value: 'INACTIVE' }]));
        sortBy('name', true);

        expect(driversRequest.latest.sort).toEqual(['name,desc']);
        expect(tableProps.latest.tableOptions.state.columnFilters).toEqual([{ id: 'status', value: 'INACTIVE' }]);
    });
});
