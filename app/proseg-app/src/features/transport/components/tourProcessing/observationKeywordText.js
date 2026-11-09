const MARKS = /\p{M}/gu;

export function normalizeObservationText(value) {
    return String(value ?? '')
        .normalize('NFD')
        .replace(MARKS, '')
        .toLowerCase()
        .trim()
        .replace(/\s+/g, ' ');
}

function normalizedMap(value) {
    const source = String(value ?? '');
    let normalized = '';
    const origins = [];
    let pendingSpace = false;
    let started = false;

    for (let index = 0; index < source.length; index += 1) {
        const character = source[index];
        if (/\s/u.test(character)) {
            if (started) pendingSpace = true;
            continue;
        }
        const folded = character.normalize('NFD').replace(MARKS, '').toLowerCase();
        if (!folded) continue;
        if (pendingSpace) {
            origins.push(index);
            normalized += ' ';
            pendingSpace = false;
        }
        for (const piece of folded) {
            origins.push(index);
            normalized += piece;
        }
        started = true;
    }

    return { normalized, origins };
}

function spansOf(observation, keyword) {
    const normalizedKeyword = normalizeObservationText(keyword);
    if (!normalizedKeyword) return [];
    const { normalized, origins } = normalizedMap(observation);
    const spans = [];
    let from = 0;
    while (from < normalized.length) {
        const at = normalized.indexOf(normalizedKeyword, from);
        if (at < 0) break;
        const end = at + normalizedKeyword.length - 1;
        spans.push([origins[at], origins[end] + 1]);
        from = at + normalizedKeyword.length;
    }
    return spans;
}

function overlaps(spans) {
    const ordered = [...spans].sort((left, right) => left[0] - right[0] || left[1] - right[1]);
    for (let index = 1; index < ordered.length; index += 1) {
        if (ordered[index][0] < ordered[index - 1][1]) return true;
    }
    return false;
}

export function highlightObservation(observation, keywords) {
    const text = observation ?? '';
    if (!text) return [];
    const spans = (keywords ?? []).flatMap((keyword) => spansOf(text, keyword));
    if (spans.length === 0 || overlaps(spans)) return [{ text, match: false }];
    const ordered = [...spans].sort((left, right) => left[0] - right[0]);
    const parts = [];
    let cursor = 0;
    ordered.forEach(([start, end]) => {
        if (start > cursor) parts.push({ text: text.slice(cursor, start), match: false });
        parts.push({ text: text.slice(start, end), match: true });
        cursor = end;
    });
    if (cursor < text.length) parts.push({ text: text.slice(cursor), match: false });
    return parts.filter((part) => part.text);
}
