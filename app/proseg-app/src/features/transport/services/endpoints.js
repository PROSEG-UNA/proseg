export const TRANSPORT_ENDPOINTS = {
    drivers: '/api/v1/transport/drivers',
    vehicles: '/api/v1/transport/vehicles',
    cleaning: '/api/v1/transport/cleaning',
    cleaningHistory: '/api/v1/transport/cleaning/history',
    cleaningDrafts: '/api/v1/transport/cleaning/drafts',
};

export const TRANSPORT_DOCUMENT_ENDPOINTS = {
    cleaningImport: '/api/v1/document-processor/imports/tours',
    cleaningImportTemplate: '/api/v1/document-processor/imports/tours/template',
    cleaningDraftExport: '/api/v1/document-processor/exports/tours/drafts',
    driverOvertimeRequestExport: '/api/v1/document-processor/exports/drivers/overtime-request',
};
