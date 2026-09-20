import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import AppTheme from '../../../common/theme/AppTheme.jsx';
import FormSelectorModal from '../components/FormSelectorModal.jsx';
import FormRecordCreateModal, { buildFormPayload } from '../components/FormRecordCreateModal.jsx';
import { formRegistry } from '../formRegistry.js';
import { AuthContext } from '../../../common/context/AuthContext.jsx';
import { SidebarContext } from '../../../common/context/SidebarContext.jsx';
import { useNavSections } from '../../../common/components/Sidebar/useNavSections.js';
import * as formsService from '../services/formsService.js';

vi.mock('../services/formsService.js', async () => {
    const actual = await vi.importActual('../services/formsService.js');
    return {
        ...actual,
        fetchFormTypes: vi.fn(),
        createFormRecord: vi.fn(),
    };
});

function renderWithProviders(ui, { permissions = [] } = {}) {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    });

    return render(
        <QueryClientProvider client={queryClient}>
            <LocalizationProvider dateAdapter={AdapterDayjs}>
                <AppTheme defaultColorScheme="light">
                    <AuthContext.Provider value={{ permissions, hasPermission: () => false, hasAnyPermission: () => false }}>
                        <SidebarContext.Provider value={{ isMinimized: false, setIsMinimized: vi.fn() }}>
                            {ui}
                        </SidebarContext.Provider>
                    </AuthContext.Provider>
                </AppTheme>
            </LocalizationProvider>
        </QueryClientProvider>
    );
}

describe('forms module', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        formsService.fetchFormTypes.mockResolvedValue([
            { id: '1', code: 'OVERTIME_REPORT', name: 'Reporte de Horas Extras' },
            { id: '2', code: 'ABSENCE_REPORT', name: 'Reporte por Ausencia' },
            { id: '3', code: 'LATE_ARRIVAL_REPORT', name: 'Reporte por Llegada Tardía' },
        ]);
        formsService.createFormRecord.mockResolvedValue({ id: 'record-1' });
    });

    it('formRegistry contiene los tres tipos', () => {
        expect(formRegistry.map((item) => item.code)).toEqual([
            'OVERTIME_REPORT',
            'ABSENCE_REPORT',
            'LATE_ARRIVAL_REPORT',
        ]);
    });

    it('selector renderiza los tres formularios', async () => {
        renderWithProviders(<FormSelectorModal open onClose={vi.fn()} onSelect={vi.fn()} />);

        expect(await screen.findByText('Reporte de Horas Extras')).toBeInTheDocument();
        expect(screen.getByText('Reporte por Ausencia')).toBeInTheDocument();
        expect(screen.getByText('Reporte por Llegada Tardía')).toBeInTheDocument();
    });

    it('formulario de Horas Extras permite agregar fila y eliminar fila', async () => {
        const user = userEvent.setup();
        renderWithProviders(
            <FormRecordCreateModal
                open
                formType={{ id: '1', code: 'OVERTIME_REPORT', name: 'Reporte de Horas Extras' }}
                onClose={vi.fn()}
                onSaved={vi.fn()}
            />
        );

        const addButton = screen.getByRole('button', { name: /agregar fila/i });
        await user.click(addButton);

        expect(screen.getAllByDisplayValue('').length).toBeGreaterThan(4);

        const deleteButtons = screen.getAllByRole('button').filter((button) => button.querySelector('svg[data-testid="DeleteOutlinedIcon"]'));
        expect(deleteButtons).toHaveLength(2);

        await user.click(deleteButtons[1]);

        const rows = screen.getAllByRole('row');
        expect(rows.length).toBeGreaterThanOrEqual(2);
    });

    it('no permite guardar detalle vacío', async () => {
        const user = userEvent.setup();
        renderWithProviders(
            <FormRecordCreateModal
                open
                formType={{ id: '1', code: 'OVERTIME_REPORT', name: 'Reporte de Horas Extras' }}
                onClose={vi.fn()}
                onSaved={vi.fn()}
            />
        );

        await user.click(screen.getByRole('button', { name: 'Guardar' }));

        expect(formsService.createFormRecord).not.toHaveBeenCalled();
        expect(screen.getByText('La fecha es obligatoria')).toBeInTheDocument();
    });

    it('calcula y actualiza total visual en horas extras', async () => {
        const user = userEvent.setup();
        renderWithProviders(
            <FormRecordCreateModal
                open
                formType={{ id: '1', code: 'OVERTIME_REPORT', name: 'Reporte de Horas Extras' }}
                onClose={vi.fn()}
                onSaved={vi.fn()}
            />
        );

        const rows = screen.getAllByRole('row');
        const firstDataRow = rows[1];
        const textboxes = within(firstDataRow).getAllByRole('textbox');

        await user.type(textboxes[0], 'Juan Perez');
        await user.type(textboxes[1], '1-1111-1111');

        const inputs = firstDataRow.querySelectorAll('input');
        await user.type(inputs[3], '17:00');
        await user.type(inputs[4], '19:30');

        expect(await screen.findByDisplayValue('2.50')).toBeInTheDocument();
    });

    it('formulario Ausencia renderiza campos requeridos y payload exacto', () => {
        renderWithProviders(
            <FormRecordCreateModal
                open
                formType={{ id: '2', code: 'ABSENCE_REPORT', name: 'Reporte por Ausencia' }}
                onClose={vi.fn()}
                onSaved={vi.fn()}
            />
        );

        expect(screen.getByLabelText('Hora')).toBeInTheDocument();
        expect(screen.getByLabelText('Guarda')).toBeInTheDocument();
        expect(screen.getByLabelText('Turno')).toBeInTheDocument();
        expect(screen.getByLabelText('Hora del turno')).toBeInTheDocument();
        expect(screen.getByLabelText('Puesto de trabajo')).toBeInTheDocument();
        expect(screen.getByLabelText('Motivo de ausencia')).toBeInTheDocument();

        expect(buildFormPayload(
            { code: 'ABSENCE_REPORT' },
            {
                date: '2026-09-13',
                time: '07:00',
                guard: 'Carlos',
                shift: 'Diurno',
                shiftTime: '07:00 - 15:00',
                workPosition: 'Acceso Norte',
                absenceReason: 'Incapacidad',
                supervisor: 'Supervisor',
            },
            []
        )).toEqual({
            fecha: '2026-09-13',
            hora: '07:00',
            guarda: 'Carlos',
            turno: 'Diurno',
            horaTurno: '07:00 - 15:00',
            puestoTrabajo: 'Acceso Norte',
            motivoAusencia: 'Incapacidad',
            supervisor: 'Supervisor',
        });
    });

    it('formulario Llegada Tardía renderiza campos requeridos y payload exacto', () => {
        renderWithProviders(
            <FormRecordCreateModal
                open
                formType={{ id: '3', code: 'LATE_ARRIVAL_REPORT', name: 'Reporte por Llegada Tardía' }}
                onClose={vi.fn()}
                onSaved={vi.fn()}
            />
        );

        expect(screen.getByLabelText('Oficial de seguridad')).toBeInTheDocument();
        expect(screen.getByLabelText('Operador de acceso vehicular')).toBeInTheDocument();
        expect(screen.getByLabelText('Hora de llegada')).toBeInTheDocument();
        expect(screen.getByLabelText('Puesto')).toBeInTheDocument();
        expect(screen.getByLabelText('Motivo')).toBeInTheDocument();

        expect(buildFormPayload(
            { code: 'LATE_ARRIVAL_REPORT' },
            {
                date: '2026-09-13',
                securityOfficer: 'Luis',
                vehicleAccessOperator: 'Ana',
                arrivalTime: '08:15',
                position: 'Puerta Principal',
                reason: 'Transito',
                supervisor: 'Supervisor',
            },
            []
        )).toEqual({
            fecha: '2026-09-13',
            oficialSeguridad: 'Luis',
            operadorAcceso: 'Ana',
            horaLlegada: '08:15',
            puesto: 'Puerta Principal',
            motivo: 'Transito',
            supervisor: 'Supervisor',
        });
    });

    it('sidebar respeta permisos de formularios', () => {
        function Probe() {
            const { sections } = useNavSections();
            return <div>{sections.map((section) => section.label).join(',')}</div>;
        }

        const { rerender } = renderWithProviders(<Probe />, { permissions: ['LEER_FORMULARIOS'] });
        expect(screen.getByText(/Formularios/)).toBeInTheDocument();

        rerender(
            <QueryClientProvider client={new QueryClient()}>
                <LocalizationProvider dateAdapter={AdapterDayjs}>
                    <AppTheme defaultColorScheme="light">
                        <AuthContext.Provider value={{ permissions: [], hasPermission: () => false, hasAnyPermission: () => false }}>
                            <SidebarContext.Provider value={{ isMinimized: false, setIsMinimized: vi.fn() }}>
                                <Probe />
                            </SidebarContext.Provider>
                        </AuthContext.Provider>
                    </AppTheme>
                </LocalizationProvider>
            </QueryClientProvider>
        );

        expect(screen.queryByText(/Formularios/)).not.toBeInTheDocument();
    });
});
