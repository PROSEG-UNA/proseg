export const TRANSPORT_ENDPOINTS = {
    drivers: '/api/v1/transport/drivers',
    vehicles: '/api/v1/transport/vehicles',
    maintenance: '/api/v1/transport/maintenance',
    tours: '/api/v1/transport/tours',
    assignment: '/api/v1/transport/assignments',
    cleaning: '/api/v1/transport/cleaning',
    cleaningHistory: '/api/v1/transport/cleaning/history',
    cleaningDrafts: '/api/v1/transport/cleaning/drafts',
};

export const TRANSPORT_DOCUMENT_ENDPOINTS = {
    cleaningImport: '/api/v1/document-processor/imports/tours',
    cleaningImportTemplate: '/api/v1/document-processor/imports/tours/template',
};
