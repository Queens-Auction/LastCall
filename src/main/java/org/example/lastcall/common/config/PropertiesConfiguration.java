package org.example.lastcall.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        MailProperties.class,
        JwtProperties.class,
        AuthProperties.class
})
public class PropertiesConfiguration {
}
