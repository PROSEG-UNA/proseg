package com.proseg.msvc_email.controller;

import com.proseg.msvc_email.notificacion.model.Email;
import com.proseg.msvc_email.notificacion.service.EmailService;
import com.proseg.msvc_email.notificacion.template.impl.GenericEmailTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.context.Context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmailControllerTest {

    private static final String BRAND = "MARCA_TEST";
    private static final String BRAND_FULL = "Programa de Prueba Completo";

    @Mock
    private EmailService emailService;

    private EmailController emailController;

    @BeforeEach
    void setUp() {
        emailController = new EmailController(emailService);
        ReflectionTestUtils.setField(emailController, "brandName", BRAND);
        ReflectionTestUtils.setField(emailController, "brandFullName", BRAND_FULL);
    }

    @Test
    @DisplayName("GET /email/test: envía GenericEmailTemplate y responde texto fijo")
    void send_cuandoGetEmailTest_enviaCorreoDePrueba() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(emailController).build();

        mockMvc.perform(get("/email/test"))
                .andExpect(status().isOk())
                // Comportamiento real: el cuerpo no incluye el destinatario (literal del controlador).
                .andExpect(content().string("Email enviado a "));

        ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
        verify(emailService).sendEmail(captor.capture());
        Email sent = captor.getValue();

        assertThat(sent.getTo()).hasSize(1);
        assertThat(sent.getSubject()).isEqualTo("Test - " + BRAND);
        assertThat(sent.getTemplateDefinition()).isInstanceOf(GenericEmailTemplate.class);
        assertThat(sent.getTemplateDefinition().getTemplateName()).isEqualTo("generic-email");

        Context ctx = sent.getTemplateDefinition().buildContext();
        assertThat(ctx.getVariable("userName")).isEqualTo("NAME");
        assertThat(ctx.getVariable("emailTitle")).isEqualTo("Notificaciones - " + BRAND);
        assertThat(ctx.getVariable("emailContent"))
                .isEqualTo("Este es un correo de prueba generado desde el microservicio de email de "
                        + BRAND_FULL + ".");
    }
}
