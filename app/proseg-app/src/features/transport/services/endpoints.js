export const TRANSPORT_ENDPOINTS = {
    drivers: '/api/v1/transport/drivers',
    vehicles: '/api/v1/transport/vehicles',
    borrowedVehicles: '/api/v1/transport/borrowed-vehicles',
    tourProcessing: '/api/v1/transport/tour-processing',
    tourProcessingDrafts: '/api/v1/transport/tour-processing/drafts',
    observationKeywords: '/api/v1/transport/observation-keywords',
    tourSplit: '/api/v1/transport/tour-split',
};

export const TRANSPORT_DOCUMENT_ENDPOINTS = {
    exports: '/api/v1/document-processor/exports',
    tourProcessingImport: '/api/v1/document-processor/imports/tours',
    tourProcessingImportTemplate: '/api/v1/document-processor/imports/tours/template',
    tourProcessingDraftExport: '/api/v1/document-processor/exports/tours/drafts',
    tourProcessingMonthExport: '/api/v1/document-processor/exports/tours/months',
    driverOvertimeRequestExport: '/api/v1/document-processor/exports/drivers/overtime-request',
};
