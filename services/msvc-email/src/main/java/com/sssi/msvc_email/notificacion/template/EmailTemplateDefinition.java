package com.sssi.msvc_email.notificacion.template;

import org.thymeleaf.context.Context;

import java.util.List;


public interface EmailTemplateDefinition {

    String getTemplateName();

    Context buildContext();

    List<String> getInlineImages();
}