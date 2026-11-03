package com.proseg.msvc_archive.security;

import com.proseg.msvc_archive.exception.ArchiveException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FolderAccessPolicy {

    private static final Map<String, String> UPLOAD_PREFIX_PRIVILEGE = Map.of(
            "assets", Privileges.SUBIR_IMAGENES_ACTIVOS,
            "tickets", Privileges.ADJUNTAR_IMAGENES_TICKETS_MANTENIMIENTO
    );

    public void checkUpload(String path, Authentication authentication) {
        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        String normalized = normalizePath(path);

        if (normalized.isEmpty()) {
            throw ArchiveException.forbidden();
        }

        String firstSegment = firstSegment(normalized);

        if ("users".equals(firstSegment)) {
            String userId = secondSegment(normalized);
            if (userId == null || !userId.equals(jwtAuth.getToken().getSubject())) {
                throw ArchiveException.forbidden();
            }
            return;
        }

        String privilege = UPLOAD_PREFIX_PRIVILEGE.get(firstSegment);
        if (privilege == null || !hasAuthority(authentication, privilege)) {
            throw ArchiveException.forbidden();
        }
    }

    public void checkRead(String objectName, Authentication authentication) {
        String normalized = normalizePath(objectName);

        if (normalized.isEmpty()
                || normalized.startsWith(".multipart/")
                || normalized.equals(".multipart")) {
            throw ArchiveException.forbidden();
        }

        if (!normalized.contains("/")) {
            requireTicketReadPrivilege(authentication);
            return;
        }

        String firstSegment = firstSegment(normalized);

        if ("assets".equals(firstSegment)) {
            if (!hasAuthority(authentication, Privileges.LEER_ACTIVOS)) {
                throw ArchiveException.forbidden();
            }
            return;
        }

        if ("tickets".equals(firstSegment)) {
            requireTicketReadPrivilege(authentication);
            return;
        }

        if ("users".equals(firstSegment)) {
            return;
        }

        throw ArchiveException.forbidden();
    }

    private void requireTicketReadPrivilege(Authentication authentication) {
        if (!hasAuthority(authentication, Privileges.LEER_TICKET_MANTENIMIENTO)
                && !hasAuthority(authentication, Privileges.LEER_TODOS_TICKETS_MANTENIMIENTO)) {
            throw ArchiveException.forbidden();
        }
    }

    private boolean hasAuthority(Authentication authentication, String privilege) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(privilege::equals);
    }

    private String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().replace("\\", "/").replaceAll("/+", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String firstSegment(String normalized) {
        int slash = normalized.indexOf('/');
        return slash < 0 ? normalized : normalized.substring(0, slash);
    }

    private String secondSegment(String normalized) {
        int firstSlash = normalized.indexOf('/');
        if (firstSlash < 0) {
            return null;
        }
        String rest = normalized.substring(firstSlash + 1);
        int secondSlash = rest.indexOf('/');
        return secondSlash < 0 ? rest : rest.substring(0, secondSlash);
    }
}
