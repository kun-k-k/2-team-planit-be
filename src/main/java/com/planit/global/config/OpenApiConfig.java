package com.planit.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Planit API",
                version = "v1",
                description = "Planit 백엔드 API 문서"
        )
)
@Configuration
public class OpenApiConfig {
}
