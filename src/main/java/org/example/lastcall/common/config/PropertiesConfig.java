package org.example.lastcall.common.config;

import org.example.lastcall.common.security.AuthProperties;
import org.example.lastcall.common.security.jwt.JwtProperties;
import org.example.lastcall.domain.auth.email.config.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        MailProperties.class,
        JwtProperties.class,
        AuthProperties.class
})
public class PropertiesConfig {}
