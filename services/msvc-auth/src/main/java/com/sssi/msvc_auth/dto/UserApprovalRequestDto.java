package com.sssi.msvc_auth.dto;

import com.sssi.msvc_auth.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserApprovalRequestDto {

    @NotNull(message = "El status es requerido")
    private User.UserStatus status;
}

