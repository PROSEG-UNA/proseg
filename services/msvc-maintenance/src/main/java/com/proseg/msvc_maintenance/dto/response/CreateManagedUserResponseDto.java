package com.proseg.msvc_maintenance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateManagedUserResponseDto {

    private String userId;
    private String username;
    private String email;
    private String status;
}

