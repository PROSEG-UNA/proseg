export function getPrivilegesColumns() {
    return [
        {
            accessorKey: 'name',
            header: 'Nombre',
            size: 140,
            grow: true,
        },
        {
            accessorKey: 'description',
            header: 'Descripción',
            size: 160,
            grow: 2,
        },
    ];
}
