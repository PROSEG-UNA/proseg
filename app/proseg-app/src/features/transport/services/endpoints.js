export const TRANSPORT_ENDPOINTS = {
    drivers: '/api/v1/transport/drivers',
    vehicles: '/api/v1/transport/vehicles',
    borrowedVehicles: '/api/v1/transport/borrowed-vehicles',
    tourProcessing: '/api/v1/transport/tour-processing',
    tourProcessingHistory: '/api/v1/transport/tour-processing/history',
    tourProcessingDrafts: '/api/v1/transport/tour-processing/drafts',
};

export const TRANSPORT_DOCUMENT_ENDPOINTS = {
    tourProcessingImport: '/api/v1/document-processor/imports/tours',
    tourProcessingImportTemplate: '/api/v1/document-processor/imports/tours/template',
    tourProcessingDraftExport: '/api/v1/document-processor/exports/tours/drafts',
    driverOvertimeRequestExport: '/api/v1/document-processor/exports/drivers/overtime-request',
};
