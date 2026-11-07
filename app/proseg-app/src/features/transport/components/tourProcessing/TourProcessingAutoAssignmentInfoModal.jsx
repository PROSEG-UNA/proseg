import { useState } from 'react';
import { Box, Chip, Stack, Tab, Tabs, Typography } from '@mui/material';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import DirectionsCarOutlinedIcon from '@mui/icons-material/DirectionsCarOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import RepeatOutlinedIcon from '@mui/icons-material/RepeatOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';

const driverStages = [
    { title: 'Preparar las giras', lead: 'Se conservan los choferes que ya traen las giras desde el catálogo, y esas giras no se reasignan. Si un nombre no existe en el catálogo, se borra para intentar asignarlo.', ending: 'Solo se consideran los choferes activos de Servicios Generales. Las giras con recurso externo se ignoran.' },
    { title: 'Asignar al solicitante', lead: 'Si el responsable de la gira coincide con un único chofer activo de Servicios Generales, la gira se le asigna a él. El nombre se compara por palabras, sin importar tildes ni mayúsculas; el nombre más corto debe tener al menos dos palabras y estar completo dentro del otro.', ending: 'Solo se asigna si el chofer cumple las reglas de horario, horas, fines de semana y 95 % de sobresueldo; de lo contrario la gira sigue el flujo normal. Estas giras ya no se mueven.' },
    { title: 'Asignar giras Periféricas', lead: 'Las giras Periféricas de Programa De Servicios Generales (destino Ulloa, en uno de los turnos fijos) se asignan a los choferes marcados como conductores de Periféricas cuyo horario ordinario coincide con el turno. Se prefiere a quien ya tenga otra gira cercana de ese turno.', ending: 'La búsqueda continúa mientras se consiga asignar alguna gira. Estas giras ya no se mueven.', badge: 'Se repite' },
    { title: 'Cubrir las obligatorias de fin de semana', lead: 'Las obligatorias son las giras de prioridad 1 con menos de 5 pasajeros; nunca se contratan y deben quedar con un chofer propio. Las que tocan sábado o domingo generan horas extra, que exigen el 95 % de sobresueldo. Para atenderlas siempre, el sistema usa la menor cantidad posible de choferes cubridores y concentra en ellos el sobresueldo:', checks: ['Elige un cubridor para cada gira: reutiliza uno ya elegido mientras sus topes lo permitan; si no, prefiere a quien lleva más sobresueldo, luego a quien puede alcanzar más y luego a quien tiene menos horas extra.', 'Le asigna giras de lunes a viernes fuera de su horario ordinario, por orden de prioridad, hasta que llegue al 95 % de su tope. En este paso no se reparte con equidad.', 'Si un cubridor no llega al 95 %, prueba pasar sus obligatorias a otro chofer que sí pueda llegar. Son pocos intentos y solo se conserva el cambio si el resultado es válido.', 'Si nadie llega, la gira se asigna igual y queda con una advertencia que indica el porcentaje de sobresueldo del chofer.'], ending: 'Una obligatoria solo queda sin chofer si nadie puede tomarla por horario, horas por día, 60 horas extra, fines de semana o vehículo; el motivo aparece en el resultado. Las obligatorias de lunes a viernes dentro de horario siguen el flujo normal, y las demás giras de fin de semana siguen exigiendo el 95 %.' },
    { title: 'Completar sobresueldo', lead: 'Se recorren las giras por prioridad para acercar a cada chofer al 95 % de su tope mensual de sobresueldo, sin aumentar las horas extra. En cada vuelta se empieza por quien lleva menos sobresueldo.', ending: 'Cada nivel de prioridad se vuelve a recorrer mientras se consigan asignaciones.', badge: 'Se repite' },
    { title: 'Asignar las giras pendientes', lead: 'Las giras se recorren por prioridad, empezando por la prioridad 1, y para cada una se descartan los choferes que no cumplen las reglas:', checks: ['Tienen otra gira que coincide en horario.', 'Pasarían de 12 horas en un día con más de una gira.', 'Pasarían de 60 horas extra en el mes o del máximo de fines de semana.', 'Tendrían horas extra sin llegar al 95 % de su sobresueldo.', 'No pueden conducir el vehículo asignado.'], ending: 'Entre los demás se prefiere a quien no tiene otra gira ese día, tiene menos horas extra, deja más tiempo entre giras y acumula menos horas. Se vuelve a recorrer lo pendiente mientras se consiga asignar alguna gira.', badge: 'Se repite' },
    { title: 'Reacomodar y volver a intentar', lead: 'Se prueba pasar una gira a otro chofer o intercambiar giras para cubrir más solicitudes y mejorar el reparto. No se mueven las giras que ya traían chofer, las del solicitante, las Periféricas ni las obligatorias de fin de semana ya cubiertas, y no se deja a un cubridor con menos sobresueldo del que ya reunió (hasta el 95 %).', ending: 'Se repite mientras haya mejoras, hasta un máximo de 20 vueltas y 10 000 intentos. Al final se vuelven a intentar las giras pendientes.', badge: 'Se repite' },
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
                    <Typography sx={{ fontSize: 14, color: 'text.secondary', mb: 3 }}>El sistema toma las giras pendientes y construye el horario de cada chofer en siete etapas.</Typography>
                    <AssignmentSteps stages={driverStages} />
                    <Stack spacing={1.5} sx={{ mt: 3, pt: 3, borderTop: '1px solid', borderColor: 'divider' }}>
                        <DetailNote title="Orden de las giras">
                            Primero van las de prioridad 1 con menos de 5 pasajeros, después las de Servicios Generales con prioridad 6 y 7, y luego las de prioridad 1 con 5 o más pasajeros. Siguen las demás prioridades en orden, con los grupos de menos de 5 pasajeros antes que los grandes. Dentro de cada nivel se ordenan por fecha y número de gira.
                        </DetailNote>
                        <DetailNote title="Giras con varios viajes">
                            Se prueban combinaciones de choferes y vehículos para completar todos los viajes. Si una combinación falla, se vuelve atrás y se prueba otra. Solo se guardan asignaciones nuevas si se completa la gira entera; de lo contrario se conservan las existentes.
                        </DetailNote>
                        <DetailNote title="Giras de varios días">
                            Una gira de varios días se asigna completa a un solo chofer, que debe estar libre todos esos días. Nunca se reparte entre choferes.
                        </DetailNote>
                        <DetailNote title="Horario ordinario y tope de sobresueldo">
                            Cada chofer tiene un solo horario ordinario, que puede ser de 07:30 a 16:30 o de 12:00 a 21:00. Las horas de lunes a viernes fuera de ese horario cuentan como sobresueldo hasta el tope individual del chofer, que depende de sus días hábiles del mes (40 horas por cada 30 días, redondeado a media hora). Lo que excede el tope pasa a horas extra.
                        </DetailNote>
                        <DetailNote title="Cuándo aparecen las horas extra">
                            Las horas de sábado y domingo siempre son horas extra, igual que lo que excede el sobresueldo. Un chofer solo recibe horas extra si ya tiene al menos el 95 % de su sobresueldo, no supera las 60 horas extra del mes y no pasa de 3 fines de semana trabajados si el mes tiene 5, o de 2 si tiene 4. La única excepción al 95 % son las obligatorias de fin de semana de la cuarta etapa, que se asignan con advertencia si no hay otra opción. La etapa de completar sobresueldo no aumenta las horas extra.
                        </DetailNote>
                        <DetailNote title="Solo recursos propios">
                            La autoasignación solo usa choferes activos de Servicios Generales. Nunca contrata transporte externo ni pide vehículos prestados; esas decisiones las toma una persona después.
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
