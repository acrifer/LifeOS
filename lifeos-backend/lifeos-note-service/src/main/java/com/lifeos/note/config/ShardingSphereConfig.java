package com.lifeos.note.config;

import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

@Configuration
public class ShardingSphereConfig {

    private final Environment environment;

    public ShardingSphereConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    @Primary
    public DataSource dataSource() throws SQLException, IOException {
        System.setProperty("javax.xml.bind.JAXBContextFactory", "com.sun.xml.bind.v2.ContextFactory");
        ClassPathResource resource = new ClassPathResource("sharding.yaml");
        String yaml = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String resolvedYaml = environment.resolvePlaceholders(yaml);
        return YamlShardingSphereDataSourceFactory.createDataSource(resolvedYaml.getBytes(StandardCharsets.UTF_8));
    }
}
