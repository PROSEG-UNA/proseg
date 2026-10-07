package com.proseg.msvc_email.notificacion.service;

import com.proseg.msvc_email.notificacion.exception.EmailSendingException;
import com.proseg.msvc_email.notificacion.exception.EmailTemplateException;
import com.proseg.msvc_email.notificacion.model.Email;
import com.proseg.msvc_email.notificacion.template.EmailTemplateDefinition;
import com.proseg.msvc_email.notificacion.util.EmailValidator;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceTest {

    private static final String SENDER = "noreply@proseg.test";
    private static final String BRAND = "PROSEG";
    private static final String BRAND_FULL = "Programa de Servicios Generales";

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private TemplateEngine templateEngine;
    @Mock
    private EmailTemplateDefinition template;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, templateEngine, new EmailValidator());
        ReflectionTestUtils.setField(emailService, "sender", SENDER);
        ReflectionTestUtils.setField(emailService, "brandName", BRAND);
        ReflectionTestUtils.setField(emailService, "brandFullName", BRAND_FULL);
    }

    private void stubEnvioBasico() {
        when(mailSender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
        when(template.getTemplateName()).thenReturn("generic-email");
        when(template.buildContext()).thenReturn(new Context());
        when(template.getInlineImages()).thenReturn(List.of());
        lenient().doReturn("<html>ok</html>").when(templateEngine).process(anyString(), any(Context.class));
    }

    private Email emailCompleto() {
        return Email.builder()
                .to(List.of("dest@b.com"))
                .cc(List.of("cc@b.com"))
                .bcc(List.of("bcc@b.com"))
                .subject("Asunto prueba")
                .templateDefinition(template)
                .build();
    }

    @Test
    @DisplayName("Envío exitoso construye MimeMessage con asunto, from, to, cc y bcc")
    void sendEmail_cuandoDatosValidos_enviaMimeMessageCorrecto() throws Exception {
        stubEnvioBasico();
        emailService.sendEmail(emailCompleto());

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        MimeMessage sent = messageCaptor.getValue();

        assertThat(sent.getSubject()).isEqualTo("Asunto prueba");
        assertThat(((InternetAddress) sent.getFrom()[0]).getAddress()).isEqualTo(SENDER);
        assertThat(recipientAddresses(sent, Message.RecipientType.TO)).containsExactly("dest@b.com");
        assertThat(recipientAddresses(sent, Message.RecipientType.CC)).containsExactly("cc@b.com");
        assertThat(recipientAddresses(sent, Message.RecipientType.BCC)).containsExactly("bcc@b.com");
    }

    @Test
    @DisplayName("El contexto de Thymeleaf incluye marca configurada")
    void sendEmail_cuandoEnvia_agregaBrandAlContexto() {
        stubEnvioBasico();
        emailService.sendEmail(emailCompleto());

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(anyString(), contextCaptor.capture());
        assertThat(contextCaptor.getValue().getVariable("brandName")).isEqualTo(BRAND);
        assertThat(contextCaptor.getValue().getVariable("brandFullName")).isEqualTo(BRAND_FULL);
    }

    @ParameterizedTest
    @MethodSource("emailsSinDestinatarios")
    @DisplayName("Sin destinatarios lanza EmailSendingException y no envía")
    void sendEmail_sinDestinatarios_lanzaExcepcion(Email sinDest) {
        assertThatThrownBy(() -> emailService.sendEmail(sinDest))
                .isInstanceOf(EmailSendingException.class)
                .hasMessageContaining("al menos un destinatario");
        verifyNoInteractions(mailSender);
    }

    static Stream<Email> emailsSinDestinatarios() {
        return Stream.of(
                Email.builder().to(null).cc(null).bcc(null).subject("Asunto")
                        .templateDefinition(mock(EmailTemplateDefinition.class)).build(),
                Email.builder().to(List.of()).cc(List.of()).bcc(List.of()).subject("Asunto")
                        .templateDefinition(mock(EmailTemplateDefinition.class)).build()
        );
    }

    @Test
    @DisplayName("Solo cc usa el sender como destinatario to")
    void sendEmail_soloCc_usaSenderComoTo() throws Exception {
        stubEnvioBasico();
        Email soloCc = Email.builder()
                .cc(List.of("cc@b.com"))
                .subject("Asunto")
                .templateDefinition(template)
                .build();

        emailService.sendEmail(soloCc);

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertThat(recipientAddresses(messageCaptor.getValue(), Message.RecipientType.TO)).containsExactly(SENDER);
        assertThat(recipientAddresses(messageCaptor.getValue(), Message.RecipientType.CC)).containsExactly("cc@b.com");
    }

    @Test
    @DisplayName("Email inválido en to lanza EmailTemplateException sin enviar")
    void sendEmail_toInvalido_lanzaEmailTemplateException() {
        Email email = Email.builder()
                .to(List.of("correo-invalido"))
                .subject("Asunto")
                .templateDefinition(template)
                .build();

        assertThatThrownBy(() -> emailService.sendEmail(email))
                .isInstanceOf(EmailTemplateException.class);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Email inválido en cc lanza EmailTemplateException sin enviar")
    void sendEmail_ccInvalido_lanzaEmailTemplateException() {
        Email email = Email.builder()
                .to(List.of("dest@b.com"))
                .cc(List.of(" "))
                .subject("Asunto")
                .templateDefinition(template)
                .build();

        assertThatThrownBy(() -> emailService.sendEmail(email))
                .isInstanceOf(EmailTemplateException.class);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Email inválido en bcc lanza EmailTemplateException sin enviar")
    void sendEmail_bccInvalido_lanzaEmailTemplateException() {
        Email email = Email.builder()
                .to(List.of("dest@b.com"))
                .bcc(List.of("sin-arroba"))
                .subject("Asunto")
                .templateDefinition(template)
                .build();

        assertThatThrownBy(() -> emailService.sendEmail(email))
                .isInstanceOf(EmailTemplateException.class);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Sender inválido lanza EmailTemplateException sin enviar")
    void sendEmail_senderInvalido_lanzaEmailTemplateException() {
        ReflectionTestUtils.setField(emailService, "sender", "no-es-email");
        Email email = Email.builder()
                .to(List.of("dest@b.com"))
                .subject("Asunto")
                .templateDefinition(template)
                .build();

        assertThatThrownBy(() -> emailService.sendEmail(email))
                .isInstanceOf(EmailTemplateException.class);
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Fallo en mailSender.send se envuelve en EmailSendingException")
    void sendEmail_cuandoSendFalla_lanzaEmailSendingExceptionConCausa() {
        stubEnvioBasico();
        doThrow(new RuntimeException("smtp caído")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailService.sendEmail(emailCompleto()))
                .isInstanceOf(EmailSendingException.class)
                .hasMessageContaining("Error enviando email")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Fallo en templateEngine.process se envuelve en EmailSendingException")
    void sendEmail_cuandoProcessFalla_lanzaEmailSendingException() {
        stubEnvioBasico();
        doThrow(new RuntimeException("plantilla rota")).when(templateEngine).process(anyString(), any(Context.class));

        assertThatThrownBy(() -> emailService.sendEmail(emailCompleto()))
                .isInstanceOf(EmailSendingException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("EmailTemplateException en buildContext se envuelve en EmailSendingException (comportamiento actual)")
    void sendEmail_buildContextFalla_envuelveEnEmailSendingException() {
        stubEnvioBasico();
        when(template.buildContext()).thenThrow(new EmailTemplateException("plantilla inválida"));

        assertThatThrownBy(() -> emailService.sendEmail(emailCompleto()))
                .isInstanceOf(EmailSendingException.class)
                .hasCauseInstanceOf(EmailTemplateException.class);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Imagen inline existente permite enviar el correo")
    void sendEmail_imagenInlineExistente_enviaCorrectamente() {
        stubEnvioBasico();
        when(template.getInlineImages()).thenReturn(List.of("flower.png"));

        emailService.sendEmail(emailCompleto());

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Imagen inline inexistente solo registra warning y aún envía (comportamiento actual)")
    void sendEmail_imagenInlineInexistente_igualEnvia() {
        stubEnvioBasico();
        when(template.getInlineImages()).thenReturn(List.of("no-existe-en-classpath.png"));

        emailService.sendEmail(emailCompleto());

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("templateDefinition null termina en EmailSendingException (comportamiento actual)")
    void sendEmail_templateNull_lanzaEmailSendingException() {
        Email email = Email.builder()
                .to(List.of("dest@b.com"))
                .subject("Asunto")
                .templateDefinition(null)
                .build();

        assertThatThrownBy(() -> emailService.sendEmail(email))
                .isInstanceOf(EmailSendingException.class)
                .hasCauseInstanceOf(NullPointerException.class);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private static List<String> recipientAddresses(MimeMessage message, Message.RecipientType type) throws Exception {
        var recipients = message.getRecipients(type);
        if (recipients == null) {
            return List.of();
        }
        return Arrays.stream(recipients)
                .map(address -> {
                    try {
                        return ((InternetAddress) address).getAddress();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

}
