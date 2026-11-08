import { useState } from 'react';
import { Box, Chip, Stack, Tab, Tabs, Typography } from '@mui/material';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import DirectionsCarOutlinedIcon from '@mui/icons-material/DirectionsCarOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import RepeatOutlinedIcon from '@mui/icons-material/RepeatOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';

const driverStages = [
    { title: 'Preparar las giras', lead: 'Se conservan los choferes asignados desde el catálogo. Si un nombre no existe en el catálogo, se borra para intentar asignarlo.', ending: 'Se consideran los choferes activos de Servicios Generales.' },
    { title: 'Asignar Ulloa', lead: 'Se buscan choferes configurados para el turno. Se prefiere a quien ya tenga otra gira cercana de ese turno.', ending: 'La búsqueda continúa mientras se consiga asignar alguna gira.', badge: 'Se repite' },
    { title: 'Completar sobresueldo', lead: 'Se recorren las giras por prioridad para acercar a cada chofer al 95 % de su tope mensual de sobresueldo, sin aumentar las horas extra.', ending: 'Cada nivel de prioridad se vuelve a recorrer mientras se consigan asignaciones.', badge: 'Se repite' },
    { title: 'Asignar las giras pendientes', lead: 'Se descartan choferes que no cumplen las reglas de la gira:', checks: ['Sus giras coinciden en horario.', 'Superarían los límites de horas.', 'No pueden conducir el vehículo asignado.'], ending: 'Entre los demás se busca conservar fines de semana libres, evitar varias giras el mismo día y repartir las horas. Se vuelve a recorrer lo pendiente mientras se consiga asignar alguna gira.', badge: 'Se repite' },
    { title: 'Reacomodar y volver a intentar', lead: 'Se prueba pasar una gira a otro chofer o intercambiar giras para cubrir más solicitudes y mejorar el reparto.', ending: 'Se repite mientras haya mejoras, hasta un máximo de 20 vueltas y 10 000 intentos. Al final se vuelven a intentar las giras pendientes.', badge: 'Se repite' },
];

const vehicleStages = [
    { title: 'Preparar las giras', lead: 'Se conservan los vehículos ya asignados desde el catálogo. Si una gira interna tiene escrito un vehículo que no está en el catálogo, se borra para intentar completarla.' },
    { title: 'Atender las giras con chofer asignado', lead: 'Primero se buscan vehículos para las giras con varios viajes que ya tienen algún chofer del catálogo. Después se atienden las giras de un solo viaje que ya tienen chofer.', ending: 'Estas giras se intentan completar antes que las giras sin chofer.' },
    { title: 'Completar las demás giras con varios viajes', lead: 'Se buscan choferes y vehículos para todos los viajes de cada gira. Si una combinación impide completar los demás viajes, se vuelve atrás y se prueba otra.', ending: 'La búsqueda termina al completar la gira, agotar las opciones o llegar a 10 000 intentos. Si falla, conserva las asignaciones existentes y no agrega viajes sueltos.', badge: 'Se repite' },
    { title: 'Asignar vehículos a las demás giras de un solo viaje', lead: 'Se recorren las giras pendientes por prioridad. Para cada una se buscan vehículos que cumplan estas condiciones:', checks: ['Tienen capacidad registrada y suficiente para los pasajeros.', 'El chofer asignado puede conducirlos, si ya hay uno.', 'Pueden quedar libres durante el horario de la gira.'], ending: 'Primero se prueban los vehículos libres, empezando por el de menor capacidad suficiente. Si ninguno sirve, se intenta cambiar vehículos elegidos durante esta autoasignación para completar la gira sin dejar otra pendiente. Este recorrido se hace una sola vez; cada búsqueda tiene un límite de 10 000 intentos.', badge: 'Una sola vez' },
    { title: 'Mostrar el resultado', lead: 'Las giras y viajes sin vehículo quedan pendientes con su motivo. Los vehículos ya asignados desde el catálogo se conservan; si no tienen capacidad registrada, no alcanzan para los pasajeros o figuran como no disponibles, aparecen en Requieren revisión.' },
];

function AssignmentSteps({ stages }) {
    return (
        <Box component="ol" sx={{ listStyle: 'none', p: 0, m: 0 }}>
            {stages.map(({ title, lead, checks, ending, badge }, index) => (
                <Box component="li" key={title} sx={{ display: 'grid', gridTemplateColumns: '32px minmax(0, 1fr)', columnGap: 2 }}>
                    <Stack alignItems="center" sx={{ height: '100%' }}>
                        <Box sx={{ width: 32, height: 32, borderRadius: '50%', bgcolor: 'primary.main', color: 'primary.contrastText', display: 'grid', placeItems: 'center', fontSize: 13, fontWeight: 700, flexShrink: 0 }}>{index + 1}</Box>
                        {index < stages.length - 1 && <Box sx={{ width: 1, flex: 1, bgcolor: 'divider', my: 1 }} />}
                    </Stack>
                    <Box sx={{ pb: index < stages.length - 1 ? 3 : 0, minWidth: 0 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', columnGap: 1.5, rowGap: 1, minHeight: 32, mb: 0.5 }}>
                            <Typography component="h4" sx={{ fontSize: 15, fontWeight: 700, lineHeight: 1.35 }}>{title}</Typography>
                            {badge && <Chip icon={badge === 'Se repite' ? <RepeatOutlinedIcon /> : undefined} label={badge} size="small" variant="outlined" color="primary" sx={{ height: 24, fontSize: 11.5 }} />}
                        </Box>
                        <Typography sx={{ fontSize: 14, lineHeight: 1.6, color: 'text.secondary', maxWidth: '72ch' }}>{lead}</Typography>
                        {checks && (
                            <Stack component="ul" spacing={0.75} sx={{ listStyle: 'none', pl: 0, my: 1.5, maxWidth: '72ch' }}>
                                {checks.map((check) => (
                                    <Stack component="li" direction="row" alignItems="flex-start" spacing={1} key={check}>
                                        <CheckCircleOutlineOutlinedIcon sx={{ fontSize: 17, color: 'success.main', mt: '3px', flexShrink: 0 }} />
                                        <Typography sx={{ fontSize: 14, lineHeight: 1.5 }}>{check}</Typography>
                                    </Stack>
                                ))}
                            </Stack>
                        )}
                        {ending && <Typography sx={{ fontSize: 14, lineHeight: 1.6, mt: checks ? 0 : 1, maxWidth: '72ch' }}>{ending}</Typography>}
                    </Box>
                </Box>
            ))}
        </Box>
    );
}

function DetailNote({ title, children }) {
    return (
        <Box sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2, bgcolor: 'background.paper', p: 2 }}>
            <Typography component="h4" sx={{ fontSize: 14, fontWeight: 700, mb: 0.75 }}>{title}</Typography>
            <Typography sx={{ fontSize: 14, lineHeight: 1.6, color: 'text.secondary' }}>{children}</Typography>
        </Box>
    );
}

export default function TourProcessingAutoAssignmentInfoModal({ open, onClose }) {
    const [activeTab, setActiveTab] = useState('drivers');
    const close = () => {
        setActiveTab('drivers');
        onClose();
    };

    return (
        <GeneralModal
            open={open}
            onClose={close}
            icon={InfoOutlinedIcon}
            title="Información de la autoasignación"
            maxWidth="md"
            primaryButton={{ label: 'Cerrar', onClick: close }}
        >
            <Tabs
                value={activeTab}
                onChange={(_, value) => setActiveTab(value)}
                variant="scrollable"
                scrollButtons="auto"
                sx={{ position: 'sticky', top: 0, zIndex: 1, px: { xs: 1.5, sm: 2 }, bgcolor: 'background.paperWarm', borderBottom: '1px solid', borderColor: 'divider' }}
            >
                <Tab id="auto-assignment-drivers-tab" aria-controls="auto-assignment-drivers-panel" value="drivers" label="Choferes" icon={<BadgeOutlinedIcon fontSize="small" />} iconPosition="start" sx={{ textTransform: 'none', fontWeight: 700, minHeight: 52 }} />
                <Tab id="auto-assignment-vehicles-tab" aria-controls="auto-assignment-vehicles-panel" value="vehicles" label="Vehículos" icon={<DirectionsCarOutlinedIcon fontSize="small" />} iconPosition="start" sx={{ textTransform: 'none', fontWeight: 700, minHeight: 52 }} />
            </Tabs>
            {activeTab === 'drivers' && (
                <Box id="auto-assignment-drivers-panel" role="tabpanel" aria-labelledby="auto-assignment-drivers-tab" sx={{ p: { xs: 2.5, sm: 3 } }}>
                    <Typography component="h3" sx={{ fontSize: 18, fontWeight: 750, mb: 0.75 }}>Autoasignación de choferes</Typography>
                    <Typography sx={{ fontSize: 14, color: 'text.secondary', mb: 3 }}>El sistema toma las giras pendientes y construye el horario de cada chofer en cinco etapas.</Typography>
                    <AssignmentSteps stages={driverStages} />
                    <Stack spacing={1.5} sx={{ mt: 3, pt: 3, borderTop: '1px solid', borderColor: 'divider' }}>
                        <DetailNote title="Giras con varios viajes">
                            Se prueban combinaciones de choferes y vehículos para completar todos los viajes. Si una combinación falla, se vuelve atrás y se prueba otra. Solo se guardan asignaciones nuevas si se completa la gira entera; de lo contrario se conservan las existentes.
                        </DetailNote>
                        <DetailNote title="Cuándo aparecen las horas extra">
                            Se calculan al probar cada gira. Las horas de fin de semana cuentan como horas extra; las horas fuera del horario ordinario entre semana se aplican primero a sobresueldo. Pueden aparecer al asignar Ulloa, las giras pendientes o durante el reacomodo, si el chofer alcanza al menos el 95 % de su tope de sobresueldo y no supera las 60 horas extra del mes. La etapa de completar sobresueldo no aumenta las horas extra.
                        </DetailNote>
                    </Stack>
                </Box>
            )}
            {activeTab === 'vehicles' && (
                <Box id="auto-assignment-vehicles-panel" role="tabpanel" aria-labelledby="auto-assignment-vehicles-tab" sx={{ p: { xs: 2.5, sm: 3 } }}>
                    <Typography component="h3" sx={{ fontSize: 18, fontWeight: 750, mb: 0.75 }}>Autoasignación de vehículos</Typography>
                    <Typography sx={{ fontSize: 14, color: 'text.secondary', mb: 3 }}>El sistema atiende primero las giras que ya tienen chofer asignado y después intenta completar las demás.</Typography>
                    <AssignmentSteps stages={vehicleStages} />
                    <Stack spacing={1.5} sx={{ mt: 3, pt: 3, borderTop: '1px solid', borderColor: 'divider' }}>
                        <DetailNote title="Sobresueldo y horas extra">
                            Las reglas de sobresueldo y horas extra se aplican cuando la etapa de varios viajes también busca choferes. La elección de vehículo para una gira de un solo viaje no agrega horas al chofer.
                        </DetailNote>
                        <DetailNote title="Vehículos prestados">
                            Si una gira necesita un vehículo prestado, debe seleccionarse manualmente. La autoasignación lo muestra como pendiente cuando falta.
                        </DetailNote>
                    </Stack>
                </Box>
            )}
        </GeneralModal>
    );
}
