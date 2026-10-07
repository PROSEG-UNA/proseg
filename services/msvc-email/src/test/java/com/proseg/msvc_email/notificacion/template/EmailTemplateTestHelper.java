package com.proseg.msvc_email.notificacion.template;

import com.proseg.msvc_email.notificacion.exception.EmailTemplateException;
import org.assertj.core.api.ThrowableAssert;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public final class EmailTemplateTestHelper {

    private EmailTemplateTestHelper() {
    }

    public static void assertRecursosEnClasspath(EmailTemplateDefinition template) {
        assertThat(new ClassPathResource("templates/email/" + template.getTemplateName() + ".html").exists()).isTrue();
        template.getInlineImages().forEach(image ->
                assertThat(new ClassPathResource("images/" + image).exists())
                        .as("classpath:images/%s", image)
                        .isTrue());
    }

    public static void assertCampoRequeridoFalla(ThrowableAssert.ThrowingCallable buildContext, String fieldName) {
        assertThatThrownBy(buildContext)
                .isInstanceOf(EmailTemplateException.class)
                .hasMessageContaining("'" + fieldName + "'");
    }

    @FunctionalInterface
    public interface TemplateFieldMutator {
        EmailTemplateDefinition withField(String fieldName, String value);
    }

    public static void assertCamposRequeridosInvalidos(TemplateFieldMutator mutator, String... fields) {
        for (String field : fields) {
            assertCampoRequeridoFalla(() -> mutator.withField(field, null).buildContext(), field);
            assertCampoRequeridoFalla(() -> mutator.withField(field, "   ").buildContext(), field);
        }
    }

}
