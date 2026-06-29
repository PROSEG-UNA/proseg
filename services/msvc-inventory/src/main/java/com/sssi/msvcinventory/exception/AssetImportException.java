package com.sssi.msvcinventory.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class AssetImportException extends BaseException {

    public AssetImportException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static AssetImportException requiredField(String fieldLabel) {
        return new AssetImportException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "IMPORT_REQUIRED_FIELD",
                "El campo obligatorio '" + fieldLabel + "' está vacío"
        );
    }

    public static AssetImportException fieldRule(String message) {
        return new AssetImportException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "IMPORT_FIELD_RULE",
                message
        );
    }

    public static AssetImportException invalidStatus() {
        return new AssetImportException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "IMPORT_INVALID_STATUS",
                "El estado no es reconocido"
        );
    }

    public static AssetImportException duplicateAssetNumber() {
        return new AssetImportException(
                HttpStatus.CONFLICT,
                "IMPORT_DUPLICATE_ASSET_NUMBER",
                "El número de activo ya existe en el sistema"
        );
    }

    public static AssetImportException duplicateAssetNumberInFile() {
        return new AssetImportException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "IMPORT_DUPLICATE_ASSET_NUMBER_IN_FILE",
                "El número de activo está duplicado dentro del archivo"
        );
    }

    public static AssetImportException duplicateSerialNumber() {
        return new AssetImportException(
                HttpStatus.CONFLICT,
                "IMPORT_DUPLICATE_SERIAL_NUMBER",
                "La serie ya existe en el sistema"
        );
    }

    public static AssetImportException duplicateSerialNumberInFile() {
        return new AssetImportException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "IMPORT_DUPLICATE_SERIAL_NUMBER_IN_FILE",
                "La serie está duplicada dentro del archivo"
        );
    }

    public static AssetImportException modelRequired() {
        return new AssetImportException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "IMPORT_MODEL_REQUIRED",
                "El modelo es obligatorio"
        );
    }

    public static AssetImportException modelNotFound() {
        return new AssetImportException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "IMPORT_MODEL_NOT_FOUND",
                "El modelo indicado no existe en el sistema"
        );
    }

    public static AssetImportException ambiguousModel() {
        return new AssetImportException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "IMPORT_AMBIGUOUS_MODEL",
                "El modelo existe para más de una marca; no se puede determinar"
        );
    }

    public static AssetImportException brandNotFound() {
        return new AssetImportException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "IMPORT_BRAND_NOT_FOUND",
                "La marca indicada no existe en el sistema"
        );
    }

    public static AssetImportException missingLocation() {
        return new AssetImportException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "IMPORT_MISSING_LOCATION",
                "La fila no contiene información de ubicación"
        );
    }

    public static AssetImportException locationNotFound() {
        return new AssetImportException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "IMPORT_LOCATION_NOT_FOUND",
                "La ubicación indicada no existe en el sistema"
        );
    }

    public static AssetImportException ambiguousLocation() {
        return new AssetImportException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "IMPORT_AMBIGUOUS_LOCATION",
                "La ubicación existe en más de un edificio o sede; no se puede determinar"
        );
    }

    public static AssetImportException buildingNotFound() {
        return new AssetImportException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "IMPORT_BUILDING_NOT_FOUND",
                "El edificio indicado no existe en el sistema"
        );
    }

    public static AssetImportException ambiguousBuilding() {
        return new AssetImportException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "IMPORT_AMBIGUOUS_BUILDING",
                "El edificio existe en más de una sede; no se puede determinar"
        );
    }

    public static AssetImportException tooManyRows(int max) {
        return new AssetImportException(
                HttpStatus.BAD_REQUEST,
                "IMPORT_TOO_MANY_ROWS",
                "La importación supera el máximo de " + max + " filas"
        );
    }
}
