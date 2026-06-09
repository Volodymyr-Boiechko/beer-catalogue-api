package com.haufe.beercatalogue.common.config;

import com.haufe.beercatalogue.security.RestAccessDeniedHandler;
import com.haufe.beercatalogue.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/manufacturers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/manufacturers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/manufacturers/**").hasAnyRole("MANUFACTURER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/beers/**").hasAnyRole("MANUFACTURER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/beers/**").hasAnyRole("MANUFACTURER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/beers/**").hasAnyRole("MANUFACTURER", "ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            );
        return http.build();
    }
}
