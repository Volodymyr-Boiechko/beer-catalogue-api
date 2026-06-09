package com.haufe.beercatalogue.common.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "basicAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "basic"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI beerCatalogueOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Beer Catalogue API")
                .description("REST API for managing beers and manufacturers")
                .version("1.0.0"))
            .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}
