import {
    Box,
    Button,
    Checkbox,
    FormControl,
    FormControlLabel,
    FormHelperText,
    FormLabel,
    IconButton,
    Paper,
    Radio,
    RadioGroup,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
    Typography,
} from '@mui/material';
import AddCircleOutlinedIcon from '@mui/icons-material/AddCircleOutlined';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { buildCard, buildRow, fromDayjs, isVisible, toDayjs } from '../formDefinitions';

const FULL_WIDTH = { gridColumn: { md: '1 / -1' } };
const GRID_SX = { display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 };

function getIn(obj, path) {
    return path.reduce((acc, key) => (acc == null ? acc : acc[key]), obj);
}

function setIn(obj, path, value) {
    if (!path.length) return value;
    const [head, ...rest] = path;
    const copy = Array.isArray(obj) ? [...obj] : { ...obj };
    copy[head] = setIn(obj?.[head], rest, value);
    return copy;
}

function ValueInput({ field, value, error, onChange, compact = false }) {
    const common = {
        fullWidth: true,
        size: compact ? 'small' : 'medium',
        label: compact ? undefined : field.label,
        error: !!error,
        helperText: error || (compact ? undefined : field.helper),
    };

    if (field.type === 'date') {
        return (
            <DatePicker
                label={compact ? undefined : field.label}
                value={toDayjs(value)}
                onChange={(next) => onChange(fromDayjs(next))}
                slotProps={{ textField: { fullWidth: true, size: common.size, error: !!error, helperText: error } }}
            />
        );
    }

    if (field.type === 'time') {
        return <TextField {...common} type="time" value={value} onChange={(e) => onChange(e.target.value)} slotProps={{ inputLabel: { shrink: true } }} />;
    }

    if (field.type === 'number') {
        return <TextField {...common} type="number" value={value} onChange={(e) => onChange(e.target.value)} slotProps={{ htmlInput: { min: field.min ?? 0, step: 1 } }} />;
    }

    if (field.type === 'textarea') {
        return <TextField {...common} value={value} onChange={(e) => onChange(e.target.value)} multiline minRows={field.minRows ?? 3} />;
    }

    return <TextField {...common} value={value} onChange={(e) => onChange(e.target.value)} />;
}

function YesNoRadios({ value, onChange }) {
    return (
        <RadioGroup row value={value}>
            {[{ value: 'SI', label: 'Sí' }, { value: 'NO', label: 'No' }].map((option) => (
                <FormControlLabel
                    key={option.value}
                    value={option.value}
                    label={option.label}
                    control={<Radio size="small" onClick={() => onChange(value === option.value ? '' : option.value)} />}
                />
            ))}
        </RadioGroup>
    );
}

function Sections({ sections, data, pathArr, ctx }) {
    return sections.map((section, index) => {
        const scopePath = section.key ? [...pathArr, section.key] : pathArr;
        const scope = getIn(data, scopePath);
        const content = (
            <Box sx={GRID_SX}>
                {section.fields.map((field) => (
                    <FieldBlock key={field.key ?? field.label} field={field} scope={scope} path={scopePath} ctx={ctx} />
                ))}
            </Box>
        );

        if (!section.title) {
            return <Box key={index}>{content}</Box>;
        }

        return (
            <Paper key={index} variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 1.5 }}>{section.title}</Typography>
                {content}
            </Paper>
        );
    });
}

function FieldBlock({ field, scope, path, ctx }) {
    const fieldPath = field.key ? [...path, field.key] : path;
    const errorKey = fieldPath.join('.');
    const error = ctx.errors[errorKey];
    const value = field.key ? scope[field.key] : undefined;

    if (!isVisible(field, scope)) return null;

    switch (field.type) {
        case 'info':
            return (
                <Typography sx={{ ...FULL_WIDTH, fontSize: 13, fontStyle: 'italic', color: 'text.secondary' }}>
                    {field.label}
                </Typography>
            );

        case 'radio':
            return (
                <FormControl error={!!error} sx={field.options.length > 2 ? FULL_WIDTH : undefined}>
                    <FormLabel>{field.label}</FormLabel>
                    <RadioGroup row value={value}>
                        {field.options.map((option) => (
                            <FormControlLabel
                                key={option.value}
                                value={option.value}
                                label={option.label}
                                control={<Radio size="small" onClick={() => ctx.setValue(fieldPath, value === option.value ? '' : option.value)} />}
                            />
                        ))}
                    </RadioGroup>
                    {error ? <FormHelperText>{error}</FormHelperText> : null}
                </FormControl>
            );

        case 'checkItem':
            return (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexWrap: 'wrap' }}>
                    <FormControlLabel
                        sx={{ minWidth: 240, m: 0 }}
                        label={field.label}
                        control={(
                            <Checkbox
                                checked={!!value.entregado}
                                onChange={(e) => ctx.setValue([...fieldPath, 'entregado'], e.target.checked)}
                            />
                        )}
                    />
                    {field.description ? (
                        <TextField
                            size="small"
                            label="Descripción"
                            value={value.descripcion}
                            onChange={(e) => ctx.setValue([...fieldPath, 'descripcion'], e.target.value)}
                            sx={{ flex: 1, minWidth: 180 }}
                        />
                    ) : null}
                </Box>
            );

        case 'yesNoItem':
            return (
                <Box sx={{ ...FULL_WIDTH, display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
                    <Typography sx={{ minWidth: 200 }}>{field.label}</Typography>
                    <YesNoRadios value={value.valor} onChange={(next) => ctx.setValue([...fieldPath, 'valor'], next)} />
                    {field.serie ? (
                        <TextField size="small" label="SERIE #" value={value.serie} onChange={(e) => ctx.setValue([...fieldPath, 'serie'], e.target.value)} />
                    ) : null}
                    {field.cantidad ? (
                        <TextField
                            size="small"
                            type="number"
                            label="Cantidad"
                            value={value.cantidad}
                            onChange={(e) => ctx.setValue([...fieldPath, 'cantidad'], e.target.value)}
                            error={!!ctx.errors[`${errorKey}.cantidad`]}
                            helperText={ctx.errors[`${errorKey}.cantidad`]}
                            slotProps={{ htmlInput: { min: 0, step: 1 } }}
                            sx={{ width: 120 }}
                        />
                    ) : null}
                </Box>
            );

        case 'table':
            return <RepeatTable field={field} rows={value} path={fieldPath} error={error} ctx={ctx} />;

        case 'cards':
            return <RepeatCards field={field} cards={value} path={fieldPath} error={error} ctx={ctx} />;

        default:
            return (
                <Box sx={field.type === 'textarea' ? FULL_WIDTH : undefined}>
                    <ValueInput field={field} value={value} error={error} onChange={(next) => ctx.setValue(fieldPath, next)} />
                </Box>
            );
    }
}

function ListHeader({ title, addLabel, onAdd, error }) {
    return (
        <>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5, gap: 2, flexWrap: 'wrap' }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>{title}</Typography>
                <Button startIcon={<AddCircleOutlinedIcon />} onClick={onAdd} sx={{ textTransform: 'none' }}>
                    {addLabel}
                </Button>
            </Box>
            {error ? <Typography color="error" sx={{ mb: 1.5, fontSize: 13 }}>{error}</Typography> : null}
        </>
    );
}

function RepeatTable({ field, rows, path, error, ctx }) {
    const renumber = (list) => (field.numberKey
        ? list.map((row, i) => ({ ...row, [field.numberKey]: i + 1 }))
        : list);

    return (
        <Box sx={FULL_WIDTH}>
            <ListHeader
                title={field.label}
                addLabel="Agregar fila"
                error={error}
                onAdd={() => ctx.setList(path, renumber([...rows, buildRow(field, {}, rows.length)]))}
            />
            <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            {field.numberKey ? <TableCell>N.°</TableCell> : null}
                            {field.columns.map((col) => <TableCell key={col.key}>{col.label}</TableCell>)}
                            <TableCell align="center">Acción</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {rows.map((row, i) => (
                            <TableRow key={i}>
                                {field.numberKey ? <TableCell sx={{ minWidth: 50 }}>{row[field.numberKey]}</TableCell> : null}
                                {field.columns.map((col) => {
                                    const cellPath = [...path, i, col.key];
                                    return (
                                        <TableCell key={col.key} sx={{ minWidth: col.type === 'time' ? 130 : 170 }}>
                                            <ValueInput
                                                compact
                                                field={col}
                                                value={row[col.key]}
                                                error={ctx.errors[cellPath.join('.')]}
                                                onChange={(next) => ctx.setValue(cellPath, next)}
                                            />
                                        </TableCell>
                                    );
                                })}
                                <TableCell align="center" sx={{ minWidth: 70 }}>
                                    <IconButton
                                        aria-label="Eliminar fila"
                                        disabled={rows.length === 1}
                                        onClick={() => ctx.setList(path, renumber(rows.filter((_, idx) => idx !== i)))}
                                    >
                                        <DeleteOutlinedIcon />
                                    </IconButton>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
}

function RepeatCards({ field, cards, path, error, ctx }) {
    return (
        <Box sx={{ ...FULL_WIDTH, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <ListHeader
                title={field.label}
                addLabel={`Agregar ${field.itemLabel.toLowerCase()}`}
                error={error}
                onAdd={() => ctx.setList(path, [...cards, buildCard(field)])}
            />
            {cards.map((card, i) => (
                <Paper key={i} variant="outlined" sx={{ p: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Typography sx={{ fontWeight: 700 }}>{`${field.itemLabel} ${i + 1}`}</Typography>
                        <IconButton
                            aria-label={`Eliminar ${field.itemLabel.toLowerCase()}`}
                            disabled={cards.length === 1}
                            onClick={() => ctx.setList(path, cards.filter((_, idx) => idx !== i))}
                        >
                            <DeleteOutlinedIcon />
                        </IconButton>
                    </Box>
                    <Sections sections={field.sections} data={card} pathArr={[]} ctx={{ ...ctx, errors: scopedErrors(ctx.errors, [...path, i]), setValue: (p, v) => ctx.setValue([...path, i, ...p], v) }} />
                </Paper>
            ))}
        </Box>
    );
}

function scopedErrors(errors, prefixPath) {
    const prefix = `${prefixPath.join('.')}.`;
    const scoped = {};
    Object.keys(errors).forEach((key) => {
        if (key.startsWith(prefix)) scoped[key.slice(prefix.length)] = errors[key];
    });
    return scoped;
}

export default function DynamicFormFields({ definition, data, setData, errors, setErrors }) {
    const clearErrors = (prefix) => setErrors((prev) => {
        const next = { ...prev };
        Object.keys(next).forEach((key) => {
            if (key === prefix || key.startsWith(`${prefix}.`)) delete next[key];
        });
        return next;
    });

    const ctx = {
        errors,
        setValue: (path, value) => {
            setData((prev) => setIn(prev, path, value));
            clearErrors(path.join('.'));
        },
        setList: (path, list) => {
            setData((prev) => setIn(prev, path, list));
            clearErrors(path.join('.'));
        },
    };

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
            <Sections sections={definition.sections} data={data} pathArr={[]} ctx={ctx} />
        </Box>
    );
}
