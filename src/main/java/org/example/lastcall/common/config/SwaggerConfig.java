package org.example.lastcall.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("프로젝트 API 문서")
                        .version("1.0.0")
                        .description("현재 프로젝트에 Swagger(OpenAPI) 적용 예시입니다."));
    }
}
