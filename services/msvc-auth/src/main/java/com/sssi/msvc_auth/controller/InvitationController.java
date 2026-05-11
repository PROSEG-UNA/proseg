package com.sssi.msvc_auth.controller;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.api.util.ApiResponseBuilder;
import com.sssi.msvc_auth.dto.InvitationInfoResponseDto;
import com.sssi.msvc_auth.dto.SetPasswordRequestDto;
import com.sssi.msvc_auth.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${routes.invitation:/api/invitations}")
@RequiredArgsConstructor
@Slf4j
public class InvitationController {

    private final InvitationService invitationService;

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<InvitationInfoResponseDto>> getInvitationInfo(
            @RequestParam String token
    ) {
        InvitationInfoResponseDto response = invitationService.getInvitationInfo(token);
        return ApiResponseBuilder.ok(
                response,
                "Información de invitación obtenida correctamente"
        );
    }

    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<Void>> activateAccount(
            @Valid @RequestBody SetPasswordRequestDto request
    ) {
        invitationService.activateUserWithToken(request.getToken(), request.getPassword());
        return ApiResponseBuilder.ok(null, "Cuenta activada correctamente");
    }

    @PostMapping("/{userId}/resend")
    public ResponseEntity<ApiResponse<Void>> resendInvitation(@PathVariable String userId) {
        log.info("Reenviando invitación para usuario {}", userId);
        invitationService.resendInvitation(userId);
        return ApiResponseBuilder.ok(null, "Invitación reenviada");
    }
}
