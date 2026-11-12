package com.proseg.msvc_transport.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {SecurityConfig.class, SecurityConfigTest.AnyRouteController.class})
class SecurityConfigTest {

    private static final String ROOT = "/api/v1/transport";

    @RestController
    static class AnyRouteController {
        @RequestMapping(ROOT + "/**")
        String ok() {
            return "ok";
        }
    }

    @Autowired
    MockMvc mvc;

    @MockBean
    JwtDecoder jwtDecoder;

    @ParameterizedTest
    @CsvSource({
            "GET,/tour-processing/imports/schema,IMPORTAR_GIRAS,200",
            "GET,/tour-processing/imports/schema,LEER_GIRAS,403",
            "GET,/tour-processing/drafts,IMPORTAR_GIRAS,200",
            "GET,/tour-processing/drafts/1,CREAR_GIRAS,200",
            "GET,/tour-processing/drafts/1/rows,EDITAR_GIRAS,200",
            "GET,/tour-processing/drafts/1/final-commission,ELIMINAR_GIRAS,200",
            "GET,/tour-processing/drafts/1/tour-control,EXPORTAR_GIRAS,200",
            "GET,/tour-processing/drafts/months/2026/5/tour-control,EXPORTAR_GIRAS,200",
            "GET,/tour-processing/drafts/months/2026/5/tour-control,LEER_CHOFERES,403",
            "GET,/tour-processing/drafts,GESTIONAR_GIRAS,403",
            "POST,/tour-processing/drafts,IMPORTAR_GIRAS,200",
            "POST,/tour-processing/drafts,CREAR_GIRAS,403",
            "POST,/tour-processing/drafts,EDITAR_GIRAS,403",
            "POST,/tour-processing/drafts/1/rows,CREAR_GIRAS,200",
            "POST,/tour-processing/drafts/1/rows,IMPORTAR_GIRAS,403",
            "POST,/tour-processing/drafts/1/rows,EDITAR_GIRAS,403",
            "POST,/tour-processing/drafts/1/rows/warnings,CREAR_GIRAS,200",
            "POST,/tour-processing/drafts/1/finish,EDITAR_GIRAS,200",
            "POST,/tour-processing/drafts/1/reopen,EDITAR_GIRAS,200",
            "POST,/tour-processing/drafts/1/split-oversized,EDITAR_GIRAS,200",
            "GET,/tour-split/keywords,LEER_GIRAS,200",
            "GET,/tour-split/settings,LEER_GIRAS,200",
            "GET,/tour-split/settings,LEER_CHOFERES,403",
            "POST,/tour-split/keywords,EDITAR_GIRAS,200",
            "POST,/tour-split/keywords,LEER_GIRAS,403",
            "PUT,/tour-split/settings,EDITAR_GIRAS,200",
            "PUT,/tour-split/keywords/1,CREAR_GIRAS,403",
            "DELETE,/tour-split/keywords/1,ELIMINAR_GIRAS,200",
            "DELETE,/tour-split/keywords/1,EDITAR_GIRAS,403",
            "POST,/tour-processing/drafts/1/assignments/clear,EDITAR_GIRAS,200",
            "POST,/tour-processing/drafts/1/drivers/auto-assign,EDITAR_GIRAS,200",
            "POST,/tour-processing/drafts/1/drivers/auto-assign-ulloa,EDITAR_GIRAS,200",
            "POST,/tour-processing/drafts/1/vehicles/auto-assign,EDITAR_GIRAS,200",
            "POST,/tour-processing/drafts/1/final-commission/reject-incomplete,EDITAR_GIRAS,200",
            "POST,/tour-processing/drafts/1/final-commission/reject-incomplete,EXPORTAR_GIRAS,403",
            "POST,/tour-processing/drafts/1/finish,CREAR_GIRAS,403",
            "PUT,/tour-processing/drafts/1/rows/2,EDITAR_GIRAS,200",
            "PUT,/tour-processing/drafts/1/rows/2,CREAR_GIRAS,403",
            "PUT,/tour-processing/drafts/1/rows/2/days/3,EDITAR_GIRAS,200",
            "PATCH,/tour-processing/drafts/1/rows/2/approval-status,EDITAR_GIRAS,200",
            "PATCH,/tour-processing/drafts/1/rows/2/approval-status,LEER_GIRAS,403",
            "DELETE,/tour-processing/drafts/1,ELIMINAR_GIRAS,200",
            "DELETE,/tour-processing/drafts/1/rows/2,ELIMINAR_GIRAS,200",
            "DELETE,/tour-processing/drafts/1/duplicates,ELIMINAR_GIRAS,200",
            "DELETE,/tour-processing/drafts/1,EDITAR_GIRAS,403",
            "GET,/drivers,LEER_CHOFERES,200",
            "GET,/drivers,GESTIONAR_CHOFERES,200",
            "GET,/drivers/months/2026/5/overtime-request,ELIMINAR_CHOFERES,200",
            "GET,/drivers,LEER_VEHICULOS,403",
            "POST,/drivers,GESTIONAR_CHOFERES,200",
            "POST,/drivers,LEER_CHOFERES,403",
            "DELETE,/drivers/1,ELIMINAR_CHOFERES,200",
            "GET,/observation-keywords,LEER_GIRAS,200",
            "GET,/observation-keywords,EDITAR_GIRAS,200",
            "GET,/observation-keywords/1,EXPORTAR_GIRAS,200",
            "GET,/observation-keywords,LEER_CHOFERES,403",
            "POST,/observation-keywords,EDITAR_GIRAS,200",
            "POST,/observation-keywords,LEER_GIRAS,403",
            "PUT,/observation-keywords/1,EDITAR_GIRAS,200",
            "PUT,/observation-keywords/1,CREAR_GIRAS,403",
            "DELETE,/observation-keywords/1,ELIMINAR_GIRAS,200",
            "DELETE,/observation-keywords/1,EDITAR_GIRAS,403",
            "PATCH,/tour-processing/drafts/1/rows/2/observation-reviewed,EDITAR_GIRAS,200",
            "PATCH,/tour-processing/drafts/1/rows/2/observation-reviewed,LEER_GIRAS,403",
            "GET,/vehicles,GESTIONAR_VEHICULOS,200",
            "GET,/borrowed-vehicles,ELIMINAR_VEHICULOS,200",
            "DELETE,/borrowed-vehicles/1,GESTIONAR_VEHICULOS,403"
    })
    void authorizesEachRouteWithItsPrivilege(String method, String path, String privilege, int expectedStatus) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), ROOT + path)
                        .with(jwt().authorities(new SimpleGrantedAuthority(privilege))))
                .andExpect(status().is(expectedStatus));
    }
}
