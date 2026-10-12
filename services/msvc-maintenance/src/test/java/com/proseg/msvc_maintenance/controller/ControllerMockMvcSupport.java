package com.proseg.msvc_maintenance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proseg.msvc_maintenance.exception.GlobalExceptionHandler;
import com.proseg.msvc_maintenance.security.Privileges;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

final class ControllerMockMvcSupport {

    static final String COMPANIES = "/api/v1/maintenance/companies";
    static final String USER_COMPANIES = "/api/v1/maintenance/user-companies";
    static final String TICKETS = "/api/v1/maintenance/tickets";
    static final String REQUESTS = "/api/v1/maintenance/requests";
    static final String REGISTERS = "/api/v1/maintenance/registers";
    static final String RECORDS = "/api/v1/maintenance/records";
    static final String LOCATIONS = "/api/v1/maintenance/locations";

    private ControllerMockMvcSupport() {
    }

    static ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    static MockMvc mockMvc(Object controller, String placeholderKey, String basePath) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        Filter principalFilter = (ServletRequest request, ServletResponse response, FilterChain chain) -> {
            if (request instanceof MockHttpServletRequest mockRequest) {
                mockRequest.setUserPrincipal(testAuthentication());
            }
            chain.doFilter(request, response);
        };

        return MockMvcBuilders.standaloneSetup(controller)
                .addPlaceholderValue(placeholderKey, basePath)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(principalFilter)
                .setCustomArgumentResolvers(
                        new TestSecurityArgumentResolver(),
                        new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper()))
                .setValidator(validator)
                .build();
    }

    static Jwt testJwt() {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("kc-user-subject")
                .claim("email", "tech@example.com")
                .build();
    }

    static Authentication testAuthentication() {
        return new TestingAuthenticationToken(
                "test-user",
                "n/a",
                Privileges.RegistrosMantenimiento.HISTORIAL);
    }

    static void expectApiError(org.springframework.test.web.servlet.ResultActions actions, int status, String errorCode)
            throws Exception {
        actions
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.errors[0]").value(errorCode));
    }

    static com.proseg.msvc_maintenance.dto.request.MaintenanceRequestRequestDto validMaintenanceRequest() {
        UUID companyId = UUID.randomUUID();
        UUID campusId = UUID.randomUUID();
        return com.proseg.msvc_maintenance.dto.request.MaintenanceRequestRequestDto.builder()
                .companyId(companyId.toString())
                .campusId(campusId.toString())
                .startDate(LocalDate.of(2026, 1, 10))
                .endDate(LocalDate.of(2026, 1, 11))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(17, 0))
                .assignedTechnicianIds(List.of(UUID.randomUUID()))
                .emails(List.of("contacto@example.com"))
                .description("Mantenimiento programado")
                .build();
    }

    private static final class TestSecurityArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            Class<?> type = parameter.getParameterType();
            if (Jwt.class.isAssignableFrom(type)) {
                return true;
            }
            return Authentication.class.isAssignableFrom(type)
                    && !Jwt.class.isAssignableFrom(type);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory) {
            if (Authentication.class.isAssignableFrom(parameter.getParameterType())) {
                return testAuthentication();
            }
            return testJwt();
        }
    }
}
