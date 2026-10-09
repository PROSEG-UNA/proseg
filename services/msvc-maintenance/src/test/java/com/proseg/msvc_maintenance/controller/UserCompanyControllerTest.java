package com.proseg.msvc_maintenance.controller;

import com.proseg.msvc_maintenance.exception.CompanyException;
import com.proseg.msvc_maintenance.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserCompanyControllerTest {

    @Mock
    private CompanyService companyService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerMockMvcSupport.mockMvc(
                new UserCompanyController(companyService),
                "routes.userCompanies",
                ControllerMockMvcSupport.USER_COMPANIES);
    }

    @Test
    @DisplayName("GET has-company responde 200 con boolean en data")
    void hasCompany_cuandoExisteEmpresa_retornaOk() throws Exception {
        when(companyService.hasCompany("kc-user-1")).thenReturn(true);

        mockMvc.perform(get(ControllerMockMvcSupport.USER_COMPANIES + "/kc-user-1/has-company"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("GET has-company propaga error del servicio")
    void hasCompany_cuandoServicioFalla_retornaNotFound() throws Exception {
        when(companyService.hasCompany("kc-user-1")).thenThrow(CompanyException.invalidKeycloakUser("kc-user-1"));

        ControllerMockMvcSupport.expectApiError(
                mockMvc.perform(get(ControllerMockMvcSupport.USER_COMPANIES + "/kc-user-1/has-company")),
                404,
                "INVALID_KEYCLOAK_USER");
    }
}
