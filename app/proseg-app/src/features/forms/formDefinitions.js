import dayjs from 'dayjs';

const text = (key, label, extra = {}) => ({ type: 'text', key, label, ...extra });
const area = (key, label, extra = {}) => ({ type: 'textarea', key, label, ...extra });
const date = (key, label, extra = {}) => ({ type: 'date', key, label, ...extra });
const time = (key, label, extra = {}) => ({ type: 'time', key, label, ...extra });
const radio = (key, label, options, extra = {}) => ({ type: 'radio', key, label, options, ...extra });
const info = (label) => ({ type: 'info', label });
const checkItem = (key, label, description = false) => ({ type: 'checkItem', key, label, description });
const yesNoItem = (key, label, extra = {}) => ({ type: 'yesNoItem', key, label, ...extra });
const req = { required: true };

const YES_NO = [{ value: 'SI', label: 'Sí' }, { value: 'NO', label: 'No' }];

const EQUIPMENT_ITEMS = [
    ['radioComunicador', 'Radio comunicador', true],
    ['bateriaRadioComunicacion', 'Batería radio de comunicación', true],
    ['cargadorBateriasRadioComunicacion', 'Cargador baterías radio de comunicación', false],
    ['antenaRadioComunicacion', 'Antena radio de comunicación', true],
    ['manosLibres', 'Manos libres', true],
    ['paraguas', 'Paraguas', false],
    ['esposas', 'Esposas', false],
    ['varaPolicial', 'Vara policial', false],
    ['controlAgujaParqueos', 'Control aguja parqueos', true],
    ['foco', 'Foco', true],
    ['bateriaFoco', 'Batería foco', true],
    ['cargadorBateriasFoco', 'Cargador baterías para foco', false],
    ['armaFuego', 'Arma de fuego', true],
    ['gorras', 'Gorras', false],
    ['cinturones', 'Cinturones', false],
];

const ROSTER_POSITIONS = [
    'Edificio Administrativo',
    'Ciencias Sociales',
    'Estudios Generales',
    'Ambientales, Puente Azul, plaza del CIDE',
    'Tierra y Mar, Auditorio Clodomiro P., hasta Informática',
    'Biología 2, Proveeduría, Energía Solar',
    'Topografía, CIDEA, CIDE',
    'OVSICORI, Mantenimiento',
    'Facultad de Filosofía y Letras',
    'Facultad de Ciencias Sociales',
    'INISEFOR',
    'Museo de Cultura Popular',
    'Edificio Financiero-Registro',
    'Colegio Humanístico',
    'Medicina Veterinaria (caseta n.° 1)',
    'Medicina Veterinaria (caseta n.° 2)',
    'Apicultura',
    'Deportes',
    'CINPE',
    'Sección de Transportes, Publicaciones',
    'Finca Santa Lucía',
    'Disponible',
    'Disponible',
    'Operador de Acceso Vehicular (Uriche)',
    'Operador de Acceso Vehicular (Biología)',
    'Operador de Acceso Vehicular (Topografía)',
];

const items = (list) => list.map(([key, label, extra]) => yesNoItem(key, label, extra));

const LOGBOOK_RECORD_SECTIONS = [
    {
        title: 'Datos del registro',
        fields: [
            text('nombre', 'Nombre', req),
            date('fecha', 'Fecha', req),
            time('hora', 'Hora'),
            text('puesto', 'Puesto'),
            text('jornada', 'Jornada'),
            text('grupoNo', 'Grupo No.'),
            text('supervisor', 'Supervisor'),
        ],
    },
    {
        title: 'Equipo de seguridad',
        key: 'equipoSeguridad',
        fields: items([
            ['armaFuego9mm', 'Arma de fuego 9 mm', { serie: true }],
            ['radioComunicacion', 'Radio de comunicación', { serie: true }],
            ['foco', 'Foco', { serie: true }],
            ['blackJack', 'Black Jack'],
            ['bateriasFoco', 'Baterías foco'],
            ['cargadorRadio', 'Cargador radio'],
            ['varaPolicial', 'Vara policial'],
            ['esposas', 'Esposas'],
            ['bateriaExtraRadio', 'Batería extra radio'],
        ]),
    },
    {
        title: 'Equipo vario',
        key: 'equipoVario',
        fields: items([
            ['llavesPuesto', 'Llaves puesto', { cantidad: true }],
            ['telefono', 'Teléfono'],
            ['extintor', 'Extintor'],
            ['pizarra', 'Pizarra'],
            ['paraguas', 'Paraguas'],
        ]),
    },
    {
        title: 'Materiales de limpieza',
        key: 'materialesLimpieza',
        fields: items([
            ['papelHigienico', 'Papel higiénico'],
            ['lavaplatos', 'Lavaplatos'],
            ['jabonPolvo', 'Jabón en polvo'],
            ['cloro', 'Cloro'],
            ['escoba', 'Escoba'],
            ['bolsaGrande', 'Bolsa grande'],
            ['isopo', 'Isopo'],
            ['limpiones', 'Limpiones'],
            ['scottBritte', 'Scott Britte'],
            ['jabonLiquido', 'Jabón líquido'],
            ['desinfectante', 'Desinfectante'],
            ['pala', 'Pala'],
            ['bolsaPequena', 'Bolsa pequeña'],
            ['paloPiso', 'Palo piso'],
        ]),
    },
    {
        title: 'Implementos de cocina',
        key: 'implementosCocina',
        fields: items([
            ['coffeMaker', 'Coffe Maker'],
            ['cuchara', 'Cuchara'],
            ['tenedor', 'Tenedor'],
            ['microondas', 'Microondas'],
            ['tasaCafe', 'Tasa café'],
            ['cuchillo', 'Cuchillo'],
            ['plato', 'Plato'],
        ]),
    },
    {
        title: 'Estado de la caseta y novedades',
        fields: [
            area('estadoCaseta', 'Estado de la caseta'),
            area('descripcionAnomaliasNovedades', 'Descripción de anomalías y novedades', { minRows: 6 }),
        ],
    },
];

export const FORM_DEFINITIONS = {
    EQUIPMENT_DELIVERY: {
        sections: [
            {
                title: 'Datos generales',
                fields: [
                    date('fecha', 'Fecha', req),
                    text('supervisorTurno', 'Supervisor de turno', req),
                    text('cedula', 'Cédula'),
                ],
            },
            {
                title: 'Equipo',
                key: 'equipo',
                fields: EQUIPMENT_ITEMS.map(([key, label, description]) => checkItem(key, label, description)),
            },
            {
                title: 'Recepción',
                fields: [
                    text('oficialSeguridadQueRecibe', 'Oficial de Seguridad que recibe'),
                    text('operadorAccesoVehicular', 'Operador de acceso vehicular'),
                    text('puesto', 'Puesto'),
                    text('cedulaRecibe', 'Cédula'),
                    text('firma', 'Firma'),
                ],
            },
        ],
    },
    POST_ANOMALIES: {
        sections: [{
            fields: [
                text('oficialSeguridadQueReporta', 'Oficial de seguridad que reporta', req),
                date('fecha', 'Fecha', req),
                text('turno', 'Turno', req),
                text('nombrePuesto', 'Nombre del puesto', req),
                text('supervisorResponsable', 'Supervisor/a responsable', req),
                area('observaciones', 'Observaciones', { ...req, minRows: 6 }),
            ],
        }],
    },
    SUPERVISOR_REPORT: {
        sections: [{
            fields: [
                text('nombre', 'Nombre', req),
                text('firma', 'Firma'),
                date('fecha', 'Fecha', req),
                text('turno', 'Turno', req),
                area('descripcion', 'Descripción', { ...req, minRows: 8 }),
            ],
        }],
    },
    SHIFT_CHANGE: {
        sections: [
            { fields: [date('fecha', 'Fecha', req)] },
            {
                title: 'Interesado/a',
                fields: [
                    text('interesado', 'Interesado/a', req),
                    date('interesadoFecha', 'Fecha', req),
                    text('interesadoHorario', 'Horario', req),
                    text('interesadoPuesto', 'Puesto', req),
                    text('interesadoFirma', 'Firma'),
                ],
            },
            {
                title: 'Sustituye',
                fields: [
                    text('sustituye', 'Sustituye', req),
                    date('sustituyeFecha', 'Fecha', req),
                    text('sustituyeHorario', 'Horario', req),
                    text('sustituyePuesto', 'Puesto', req),
                    text('sustituyeFirma', 'Firma'),
                ],
            },
            {
                fields: [
                    area('justificacion', 'Justificación', req),
                    radio('estado', 'Estado', [
                        { value: 'AUTORIZADO', label: 'Autorizado' },
                        { value: 'NO_AUTORIZADO', label: 'No autorizado' },
                    ]),
                    text('firmaJefatura', 'Firma Jefatura'),
                    text('supervisorInteresado', 'Nombre del/de la Supervisor/a del/de la interesado/a'),
                    text('supervisorInteresadoFirma', 'Firma'),
                    text('supervisorSustituye', 'Nombre del/de la Supervisor/a del/de la que sustituye'),
                    text('supervisorSustituyeFirma', 'Firma'),
                    info('IMPORTANTE: Este cambio debe solicitarse con 3 días de antelación y quedará sin efecto si alguno/a de los/as interesados/as presenta incapacidad.'),
                ],
            },
        ],
    },
    VACATION_PERMIT_REQUEST: {
        sections: [
            {
                title: 'Solicitante',
                fields: [
                    text('nombreSolicitante', 'Nombre del/de la solicitante', req),
                    text('cedula', 'Cédula #', req),
                    text('grupo', 'Grupo #'),
                ],
            },
            {
                title: 'Trámite solicitado',
                fields: [
                    radio('tramiteSolicitado', 'Trámite solicitado', [
                        { value: 'VACACIONES', label: 'Vacaciones' },
                        { value: 'PERMISO', label: 'Permiso' },
                        { value: 'OTRO', label: 'Otro' },
                    ], req),
                    text('otroEspecifique', 'Otro (especifique)', { visibleWhen: { key: 'tramiteSolicitado', equals: 'OTRO' }, requiredWhen: { key: 'tramiteSolicitado', equals: 'OTRO' } }),
                    date('fechaDesde', 'Fecha desde', req),
                    date('fechaHasta', 'Fecha hasta', req),
                    date('fechaRegreso', 'Fecha de regreso'),
                    { type: 'number', key: 'cantidadDias', label: 'Cantidad de días solicitados', required: true, min: 1 },
                    area('observaciones', 'Observaciones'),
                    date('fechaSolicitud', 'Fecha cuando realiza la solicitud'),
                    text('firmaSolicitante', 'Firma del/de la solicitante'),
                ],
            },
            {
                title: 'Espacio para uso del/de la supervisor/a',
                fields: [
                    date('fechaRecibido', 'Fecha de recibido'),
                    time('horaRecibido', 'Hora de recibido'),
                    area('explicacionNoAutoriza', 'Explicación de por qué no se autoriza la solicitud'),
                    radio('autorizado', 'Autorizado', YES_NO),
                    text('firmaSupervisor', 'Firma del/de la supervisor/a'),
                    info('NOTA: ESTA SOLICITUD NO IMPLICA LA APROBACIÓN DEL TRÁMITE. ESTE QUEDA SUJETO AL V.° B.° DEL/DE LA SUPERVISOR/A'),
                ],
            },
            {
                title: 'Espacio para uso de la secretaría',
                fields: [
                    text('secretariaNombreSolicitante', 'Nombre del/de la solicitante'),
                    text('secretariaFechasSolicitadas', 'Fechas solicitadas'),
                    text('secretariaFirmaHoraFechaRecibido', 'Firma, hora y fecha de recibido'),
                ],
            },
        ],
    },
    SERVICE_ROSTER: {
        sections: [
            {
                title: 'Datos generales',
                fields: [
                    text('grupo', 'Grupo', req),
                    time('turnoServicioDesde', 'Turno de servicio desde', req),
                    time('turnoServicioHasta', 'Turno de servicio hasta', req),
                    date('fecha', 'Fecha', req),
                    text('nombreSupervisor', 'Nombre del/de la supervisor/a', req),
                    text('firma', 'Firma'),
                ],
            },
            {
                fields: [{
                    type: 'table',
                    key: 'detalle',
                    label: 'Detalle',
                    numberKey: 'numero',
                    defaultRows: ROSTER_POSITIONS.map((puestoAsignado) => ({ puestoAsignado })),
                    columns: [
                        text('puestoAsignado', 'Puesto asignado', req),
                        text('nombreOficialSeguridad', 'Nombre del/de la oficial de seguridad'),
                        text('motivo', 'Motivo'),
                    ],
                }],
            },
            { fields: [area('observaciones', 'Observaciones')] },
        ],
    },
    ACCESS_CONTROL: {
        sections: [
            { fields: [date('fecha', 'Fecha', req), text('puesto', 'Puesto', req)] },
            {
                fields: [{
                    type: 'table',
                    key: 'detalle',
                    label: 'Detalle',
                    columns: [
                        text('numeroPlaca', 'N° placa'),
                        text('nombrePersona', 'Nombre de la persona', req),
                        text('numeroCedula', 'N° cédula', req),
                        text('oficinaVisitar', 'Oficina a visitar'),
                        { type: 'phone', key: 'numeroTelefono', label: 'Número teléfono' },
                    ],
                }],
            },
        ],
    },
    DAILY_ROUNDS_CONTROL: {
        sections: [
            {
                title: 'Supervisores de la Sección de Vigilancia',
                fields: [
                    text('nombreSupervisorTurno', 'Nombre del supervisor de turno', req),
                    text('firma', 'Firma'),
                    time('turnoDesde', 'Turno de las', req),
                    time('turnoHasta', 'a las', { ...req, helper: 'horas' }),
                    text('grupo', 'Grupo', req),
                    date('fecha', 'Fecha', req),
                ],
            },
            {
                fields: [{
                    type: 'table',
                    key: 'detalle',
                    label: 'Detalle',
                    columns: [
                        text('puesto', 'Puesto', req),
                        time('hora', 'Hora', req),
                        text('nombreOficialSeguridad', 'Nombre oficial de seguridad', req),
                        text('firmaOficialSeguridad', 'Firma oficial de seguridad'),
                    ],
                }],
            },
        ],
    },
    LOGBOOK: {
        sections: [
            {
                title: 'Identificación de la bitácora',
                fields: [
                    text('numeroBitacora', 'Número de bitácora', req),
                    text('puesto', 'Puesto', req),
                    date('vigenciaDesde', 'Vigencia desde (día / mes / año)', req),
                    date('vigenciaHasta', 'Vigencia hasta (día / mes / año)', req),
                ],
            },
            {
                fields: [{
                    type: 'cards',
                    key: 'registros',
                    label: 'Registros',
                    itemLabel: 'Registro',
                    sections: LOGBOOK_RECORD_SECTIONS,
                }],
            },
        ],
    },
};

const isIsoDate = (value) => /^\d{4}-\d{2}-\d{2}$/.test(value ?? '');

function emptyValue(field) {
    switch (field.type) {
        case 'checkItem': return { entregado: false, descripcion: '' };
        case 'yesNoItem': return { valor: '', serie: '', cantidad: '' };
        case 'table': return [];
        case 'cards': return [];
        default: return '';
    }
}

function buildSections(sections) {
    const data = {};
    sections.forEach((section) => {
        const target = {};
        section.fields.forEach((field) => {
            if (field.type === 'info') return;
            if (field.type === 'table') {
                const rows = field.defaultRows?.length ? field.defaultRows : [{}];
                target[field.key] = rows.map((row, index) => buildRow(field, row, index));
            } else if (field.type === 'cards') {
                target[field.key] = [buildSections(field.sections)];
            } else {
                target[field.key] = emptyValue(field);
            }
        });
        if (section.key) data[section.key] = target;
        else Object.assign(data, target);
    });
    return data;
}

export function buildRow(table, seed = {}, index = 0) {
    const row = {};
    if (table.numberKey) row[table.numberKey] = index + 1;
    table.columns.forEach((col) => { row[col.key] = seed[col.key] ?? ''; });
    return row;
}

export const buildCard = (card) => buildSections(card.sections);

export function buildInitialData(definition) {
    return buildSections(definition.sections);
}

const isRequired = (field, data) => field.required
    || (field.requiredWhen && data[field.requiredWhen.key] === field.requiredWhen.equals);

export const isVisible = (field, data) => !field.visibleWhen
    || data[field.visibleWhen.key] === field.visibleWhen.equals;

function validateValue(field, value, data, path, errors) {
    const empty = value === '' || value === null || value === undefined;
    if (empty) {
        if (isRequired(field, data)) errors[path] = `${field.label} es obligatorio`;
        return;
    }
    if (field.type === 'number' && (!Number.isInteger(Number(value)) || Number(value) < (field.min ?? 0))) {
        errors[path] = `Debe ser un entero mayor o igual a ${field.min ?? 0}`;
    }
    if (field.type === 'phone' && !/^[0-9+()\-\s]{7,20}$/.test(String(value).trim())) {
        errors[path] = 'Teléfono no válido';
    }
    if (field.type === 'date' && !isIsoDate(value)) {
        errors[path] = 'Fecha no válida';
    }
}

function validateSections(sections, data, base, errors) {
    sections.forEach((section) => {
        const scope = section.key ? data[section.key] : data;
        const scopePath = section.key ? `${base}${section.key}.` : base;
        section.fields.forEach((field) => {
            if (field.type === 'info' || !isVisible(field, scope)) return;
            const path = `${scopePath}${field.key}`;
            if (field.type === 'table') {
                const rows = scope[field.key] ?? [];
                if (!rows.length) errors[path] = 'Debe existir al menos una fila';
                rows.forEach((row, i) => field.columns.forEach((col) => (
                    validateValue(col, row[col.key], row, `${path}.${i}.${col.key}`, errors)
                )));
            } else if (field.type === 'cards') {
                const cards = scope[field.key] ?? [];
                if (!cards.length) errors[path] = 'Debe existir al menos un registro';
                cards.forEach((card, i) => validateSections(field.sections, card, `${path}.${i}.`, errors));
            } else if (field.type !== 'checkItem' && field.type !== 'yesNoItem') {
                validateValue(field, scope[field.key], scope, path, errors);
            } else if (field.type === 'yesNoItem' && scope[field.key].cantidad !== '') {
                const quantity = Number(scope[field.key].cantidad);
                if (!Number.isInteger(quantity) || quantity < 0) errors[`${path}.cantidad`] = 'Debe ser un entero mayor o igual a 0';
            }
        });
    });
}

export function validateDefinition(definition, data) {
    const errors = {};
    validateSections(definition.sections, data, '', errors);

    if (data.fechaDesde && data.fechaHasta && data.fechaHasta < data.fechaDesde) {
        errors.fechaHasta = 'La fecha hasta no puede ser anterior a la fecha desde';
    }
    if (data.vigenciaDesde && data.vigenciaHasta && data.vigenciaHasta < data.vigenciaDesde) {
        errors.vigenciaHasta = 'La vigencia hasta no puede ser anterior a la vigencia desde';
    }
    return errors;
}

function cleanField(field, value) {
    switch (field.type) {
        case 'date':
        case 'time':
            return value || null;
        case 'number':
            return value === '' || value === null ? null : Number(value);
        case 'yesNoItem': {
            const out = { valor: value.valor || null };
            if (field.serie) out.serie = value.serie || '';
            if (field.cantidad) out.cantidad = value.cantidad === '' ? null : Number(value.cantidad);
            return out;
        }
        case 'checkItem':
            return field.description
                ? { entregado: !!value.entregado, descripcion: value.descripcion || '' }
                : { entregado: !!value.entregado };
        default:
            return value;
    }
}

function cleanSections(sections, data) {
    const out = {};
    sections.forEach((section) => {
        const scope = section.key ? data[section.key] : data;
        const target = {};
        section.fields.forEach((field) => {
            if (field.type === 'info' || !isVisible(field, scope)) return;
            const value = scope[field.key];
            if (field.type === 'table') {
                target[field.key] = value.map((row) => {
                    const cleaned = {};
                    if (field.numberKey) cleaned[field.numberKey] = row[field.numberKey];
                    field.columns.forEach((col) => { cleaned[col.key] = cleanField(col, row[col.key]); });
                    return cleaned;
                });
            } else if (field.type === 'cards') {
                target[field.key] = value.map((card) => cleanSections(field.sections, card));
            } else {
                target[field.key] = cleanField(field, value);
            }
        });
        if (section.key) out[section.key] = target;
        else Object.assign(out, target);
    });
    return out;
}

export function buildDefinitionPayload(definition, data) {
    return cleanSections(definition.sections, data);
}

export const toDayjs = (value) => (isIsoDate(value) ? dayjs(value) : null);
export const fromDayjs = (value) => (value && value.isValid() ? value.format('YYYY-MM-DD') : '');
