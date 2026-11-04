package com.proseg.msvc_document_processor.config;

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

    private static final String ROOT = "/api/v1/document-processor";

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
            "POST,/imports/tours,IMPORTAR_GIRAS,200",
            "POST,/imports/tours,EDITAR_GIRAS,403",
            "GET,/imports/tours/template,IMPORTAR_GIRAS,200",
            "GET,/imports/tours/template,LEER_GIRAS,403",
            "GET,/exports/tours/drafts/1,EXPORTAR_GIRAS,200",
            "GET,/exports/tours/drafts/1/final-commission,EXPORTAR_GIRAS,200",
            "GET,/exports/tours/drafts/1/tour-control,LEER_GIRAS,403",
            "GET,/exports/drivers/overtime-request,LEER_CHOFERES,200",
            "GET,/exports/drivers/overtime-request,EXPORTAR_GIRAS,403"
    })
    void authorizesEachTourRouteWithItsPrivilege(String method, String path, String privilege, int expectedStatus) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), ROOT + path)
                        .with(jwt().authorities(new SimpleGrantedAuthority(privilege))))
                .andExpect(status().is(expectedStatus));
    }
}
