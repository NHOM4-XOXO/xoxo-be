package com.nhom4.xoxo.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("XOXO Social Media API")
                                                .description("REST API for XOXO Social Media Platform with OAuth2 and JWT Authentication")
                                                .version("1.0.0")
                                                .contact(new Contact()
                                                                .name("XOXO Team")
                                                                .email("contact@xoxo.com")
                                                                .url("https://xoxo.com"))
                                                .license(new License()
                                                                .name("MIT License")
                                                                .url("https://opensource.org/licenses/MIT")))
                                // .servers(List.of(
                                //                 new Server().url("https://localhost:8443")
                                //                                 .description("Development Server"),
                                //                 new Server().url("https://xoxo.id.vn:443")
                                //                                 .description("Production Server"),

                                //                 new Server().url("https://xoxo.id.vn")
                                //                                 .description("Production Server")))
                                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                                .components(new Components()
                                                .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                                                .type(SecurityScheme.Type.HTTP)
                                                                .scheme("bearer")
                                                                .bearerFormat("JWT")
                                                                .description("Enter JWT token from OAuth2 login")))

                ;
        }
}