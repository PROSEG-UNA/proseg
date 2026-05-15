package com.sssi.msvcinventory.exception;

import com.sssi.common.api.exception.BaseException;
import org.springframework.http.HttpStatus;

public class NetworkInterfaceException extends BaseException {

    public NetworkInterfaceException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }

    public static NetworkInterfaceException notFound(String id) {
        return new NetworkInterfaceException(
                HttpStatus.NOT_FOUND,
                "NETWORK_INTERFACE_NOT_FOUND",
                "Interfaz de red no encontrada con id: " + id
        );
    }

    public static NetworkInterfaceException duplicateIp(String ip) {
        return new NetworkInterfaceException(
                HttpStatus.CONFLICT,
                "NETWORK_INTERFACE_DUPLICATE_IP",
                "Ya existe una IP y MAC con la dirección IP: " + ip
        );
    }

    public static NetworkInterfaceException duplicateMac(String mac) {
        return new NetworkInterfaceException(
                HttpStatus.CONFLICT,
                "NETWORK_INTERFACE_DUPLICATE_MAC",
                "Ya existe una IP y MAC con la dirección MAC: " + mac
        );
    }

    public static NetworkInterfaceException assetAlreadyHasInterface(String assetId) {
        return new NetworkInterfaceException(
                HttpStatus.CONFLICT,
                "NETWORK_INTERFACE_ASSET_CONFLICT",
                "El activo con id: " + assetId + " ya tiene una IP y MAC asignada"
        );
    }

    public static NetworkInterfaceException requiredByAssetType(String assetTypeName) {
        return new NetworkInterfaceException(
                HttpStatus.CONFLICT,
                "NETWORK_INTERFACE_REQUIRED",
                "No se puede eliminar la IP y MAC: el tipo de activo '" + assetTypeName + "' la requiere"
        );
    }
}