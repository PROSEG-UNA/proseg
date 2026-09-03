package com.proseg.msvc_archive.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

@SuppressWarnings("unused")
public class MinioEnabledCondition implements Condition {

    @Override
    public boolean matches(@NonNull ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
        var env = context.getEnvironment();
        return StringUtils.hasText(env.getProperty("minio.url"))
                && StringUtils.hasText(env.getProperty("minio.access-key"))
                && StringUtils.hasText(env.getProperty("minio.secret-key"));
    }
}

