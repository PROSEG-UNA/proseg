package com.sssi.msvc_auth.controller;

import com.sssi.msvc_auth.dto.AuthResponse;
import com.sssi.msvc_auth.dto.LoginRequest;
import com.sssi.msvc_auth.dto.RegisterRequest;
import com.sssi.msvc_auth.service.KeycloakAdminService;
import com.sssi.msvc_auth.service.KeycloakAuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.sssi.msvc_auth.dto.CreateRoleRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${routes.auth}")
@Slf4j
public class AuthController {

    @Autowired
    private KeycloakAuthService keycloakAuthService;

    @Autowired
    private KeycloakAdminService keycloakAdminService;


    @GetMapping
    public String hello() {
        return "Auth Service is running";
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            log.info("Login attempt for user: {}", loginRequest.getUsername());

            String token = keycloakAuthService.getToken(loginRequest.getUsername(), loginRequest.getPassword());

            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setUsername(loginRequest.getUsername());
            response.setSuccess(true);
            response.setMessage("Login exitoso");

            log.info("Login exitoso para usuario: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error en login: ", e);
            AuthResponse response = new AuthResponse();
            response.setSuccess(false);
            response.setMessage("Credenciales inválidas: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            log.info("Register attempt for user: {}", registerRequest.getUsername());

            if (keycloakAdminService.userExists(registerRequest.getUsername())) {
                AuthResponse response = new AuthResponse();
                response.setSuccess(false);
                response.setMessage("El usuario ya existe");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            keycloakAdminService.registerUser(
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword(),
                    registerRequest.getFirstName(),
                    registerRequest.getLastName());

            String token = keycloakAuthService.getToken(registerRequest.getUsername(), registerRequest.getPassword());

            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setUsername(registerRequest.getUsername());
            response.setEmail(registerRequest.getEmail());
            response.setSuccess(true);
            response.setMessage("Registro exitoso. Por favor inicie sesión.");

            log.info("Registro exitoso para usuario: {}", registerRequest.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error en registro: ", e);
            AuthResponse response = new AuthResponse();
            response.setSuccess(false);
            response.setMessage("Error en el registro: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/roles")
    public ResponseEntity<?> getRoles() {
        try {
            List<Map<String, Object>> roles = keycloakAdminService.getRoles();
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            log.error("Error obteniendo roles: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener roles: " + e.getMessage());
        }
    }

    @PostMapping("/roles")
    public ResponseEntity<?> createRole(@Valid @RequestBody CreateRoleRequest request) {
        try {
            keycloakAdminService.createCompositeRole(request.getRoleName(), request.getPrivileges());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Rol " + request.getRoleName() + " creado exitosamente");
        } catch (Exception e) {
            log.error("Error creando rol: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al crear rol: " + e.getMessage());
        }
    }

    @PutMapping("/roles/{roleName}")
    public ResponseEntity<?> updateRole(@PathVariable String roleName,
                                        @Valid @RequestBody CreateRoleRequest request) {
        try {
            keycloakAdminService.updateRole(roleName, request.getPrivileges());
            return ResponseEntity.ok("Rol " + roleName + " actualizado exitosamente");
        } catch (Exception e) {
            log.error("Error actualizando rol: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al actualizar rol: " + e.getMessage());
        }
    }

    @DeleteMapping("/roles/{roleName}")
    public ResponseEntity<?> deleteRole(@PathVariable String roleName) {
        try {
            keycloakAdminService.deleteRole(roleName);
            return ResponseEntity.ok("Rol " + roleName + " eliminado exitosamente");
        } catch (Exception e) {
            log.error("Error eliminando rol: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al eliminar rol: " + e.getMessage());
        }
    }
}
