package org.example.lastcall.common.config;

import java.util.List;
import org.example.lastcall.common.security.AuthUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // AuthUserArgumentResolver를 Spring MVC에 등록
        resolvers.add(new AuthUserArgumentResolver());
    }
}

