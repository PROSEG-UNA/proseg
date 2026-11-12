const MAX_VALUE_LENGTH = 40;

function shorten(text) {
    return text.length > MAX_VALUE_LENGTH ? `${text.slice(0, MAX_VALUE_LENGTH)}...` : text;
}

export function fieldChangeText(label, before, after, format = (value) => value) {
    if (before === after) return null;
    if (!before) return `${label}: se agregó ${shorten(format(after))}`;
    if (!after) return `${label}: se quitó (antes era ${shorten(format(before))})`;
    return `${label}: de ${shorten(format(before))} a ${shorten(format(after))}`;
}
