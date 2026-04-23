package com.sssi.msvc_auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequestDto {
    private String roleName;
    private String description;
    private List<String> privileges;
}