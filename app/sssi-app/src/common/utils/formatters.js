export const formatRoleName = (name) => {
    if (!name) return name;
    return name
        .split('_')
        .map((word, i) => i === 0
            ? word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
            : word.toLowerCase()
        )
        .join(' ');
};
