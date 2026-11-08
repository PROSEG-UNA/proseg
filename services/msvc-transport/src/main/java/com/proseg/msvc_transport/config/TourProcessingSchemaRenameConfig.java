package com.proseg.msvc_transport.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Stream;

@Configuration(proxyBeanMethods = false)
public class TourProcessingSchemaRenameConfig {

    @Bean
    static BeanFactoryPostProcessor tourProcessingSchemaRenameDependency() {
        return beanFactory -> {
            var definition = beanFactory.getBeanDefinition("entityManagerFactory");
            String[] existing = definition.getDependsOn();
            definition.setDependsOn(Stream.concat(
                    existing == null ? Stream.empty() : Arrays.stream(existing),
                    Stream.of("tourProcessingSchemaRename")
            ).toArray(String[]::new));
        };
    }

    @Bean
    InitializingBean tourProcessingSchemaRename(DataSource dataSource) {
        return () -> {
            String script = new ClassPathResource("db/transport-tour-processing-rename.sql")
                    .getContentAsString(StandardCharsets.UTF_8);
            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("select pg_advisory_xact_lock(8675022026)");
                    statement.execute(script);
                    connection.commit();
                } catch (Exception ex) {
                    connection.rollback();
                    throw ex;
                }
            }
        };
    }
}
