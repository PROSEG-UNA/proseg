package com.sssi.msvcinventory.importer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum AssetImportField {

    ASSET_NUMBER("assetNumber", ColumnType.TEXT,
            List.of("Número de Activo", "Número Activo", "Núm. Activo", "No. Activo", "N° Activo", "Nro. Activo",
                    "Código de Activo", "Código Activo",
                    "Número de Etiqueta", "Número Etiqueta", "Etiqueta", "Código de Etiqueta",
                    "Placa", "Placa de Activo", "Número de Placa",
                    "Activo")),
    SERIAL_NUMBER("serialNumber", ColumnType.TEXT,
            List.of("Número de Serie", "Número Serie", "Núm. Serie", "No. Serie", "N° Serie", "Nro. Serie",
                    "Serie", "Serial", "Número de Serial")),
    TYPE_NAME("typeName", ColumnType.TEXT,
            List.of("Tipo Activo", "Tipo", "Tipo de Producto", "Tipo Producto", "Tipo del Producto",
                    "Tipo de Activo", "Tipo de Equipo",
                    "Categoría", "Clase", "Descripción")),
    BRAND_NAME("brandName", ColumnType.TEXT,
            List.of("Marca", "Fabricante")),
    MODEL_NAME("modelName", ColumnType.TEXT,
            List.of("Modelo", "Modelo de Equipo", "Modelo del Equipo")),
    CAMPUS_NAME("campusName", ColumnType.TEXT,
            List.of("Campus", "Sede", "Recinto")),
    BUILDING_NAME("buildingName", ColumnType.TEXT,
            List.of("Edificio", "Nombre de Edificio", "Nombre del Edificio", "Nombre Edificio")),
    FLOOR_NAME("floorName", ColumnType.TEXT,
            List.of("Piso", "Planta", "Nivel")),
    LOCATION_NAME("locationName", ColumnType.TEXT,
            List.of("Detalle de Ubicación", "Detalle Ubicación", "Detalle de la Ubicación",
                    "Descripción de Ubicación", "Descripción de la Ubicación", "Descripción Ubicación",
                    "Ubicación", "Ubicación Física", "Espacio", "Locación", "Lugar")),
    STATUS("status", ColumnType.TEXT,
            List.of("Estado", "Estado del Activo", "Estado Activo", "Estado de Activo",
                    "Estado del Equipo", "Condición")),
    EXECUTING_UNIT("executingUnit", ColumnType.TEXT,
            List.of("Unidad Ejecutora", "Unidad", "Unidad Académica")),
    RESPONSIBLE_EMPLOYEE_ID("responsibleEmployeeId", ColumnType.TEXT,
            List.of(
                    "Identificación Funcionario", "Identificación de Funcionario", "Identificación del Funcionario",
                    "Cédula Funcionario", "Cédula de Funcionario", "Cédula del Funcionario",
                    "ID Funcionario", "ID de Funcionario", "ID del Funcionario",

                    "Identificación Encargado", "Identificación de Encargado", "Identificación del Encargado",
                    "Cédula Encargado", "Cédula de Encargado", "Cédula del Encargado",
                    "ID Encargado", "ID de Encargado", "ID del Encargado",

                    "Identificación Responsable", "Identificación de Responsable", "Identificación del Responsable",
                    "Cédula Responsable", "Cédula de Responsable", "Cédula del Responsable",
                    "ID Responsable", "ID de Responsable", "ID del Responsable"
                    )),
    RESPONSIBLE_EMPLOYEE("responsibleEmployee", ColumnType.TEXT,
            List.of("Nombre Funcionario", "A Nombre De",
                    "Funcionario", "Nombre de Funcionario", "Nombre del Funcionario",
                    "Encargado", "Nombre Encargado", "Nombre de Encargado", "Nombre del Encargado",
                    "Responsable", "Nombre Responsable", "Nombre de Responsable", "Nombre del Responsable")),
    ACQUISITION_DATE("acquisitionDate", ColumnType.DATE,
            List.of("Fecha de Adquisición", "Fecha Adquisición", "Fecha de la Adquisición",
                    "Fecha de Compra", "Fecha Compra", "Fecha de la Compra")),
    WARRANTY_END_DATE("warrantyEndDate", ColumnType.DATE,
            List.of("Vencimiento de Garantía", "Vencimiento Garantía", "Vencimiento de la Garantía",
                    "Fin de Garantía", "Fin Garantía", "Fin de la Garantía",
                    "Fecha de Garantía", "Fecha Garantía", "Fecha Fin Garantía",
                    "Fecha de Vencimiento de Garantía", "Garantía Hasta")),
    FIRMWARE_SUPPORT_END_DATE("firmwareSupportEndDate", ColumnType.DATE,
            List.of("Fin Soporte Firmware", "Fin de Soporte Firmware", "Fin del Soporte Firmware",
                    "Fin Soporte de Firmware",
                    "Soporte Hasta", "Fecha Fin Soporte", "Fecha de Fin de Soporte")),
    LATITUDE("latitude", ColumnType.DECIMAL,
            List.of("Latitud")),
    LONGITUDE("longitude", ColumnType.DECIMAL,
            List.of("Longitud")),
    IP_ADDRESS("ipAddress", ColumnType.TEXT,
            List.of("Dirección IP", "IP", "Dir. IP")),
    MAC_ADDRESS("macAddress", ColumnType.TEXT,
            List.of("Dirección MAC", "MAC", "Dir. MAC", "Dirección Física"));

    private final String attribute;
    private final ColumnType type;
    private final List<String> names;
}
