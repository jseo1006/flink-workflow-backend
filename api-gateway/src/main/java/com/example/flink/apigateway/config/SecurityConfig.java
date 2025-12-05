package com.example.flink.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for API simplicity in this demo
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/login", "/logout").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().authenticated())
                .formLogin(formLogin -> {
                })
                .httpBasic(httpBasic -> {
                });
        return http.build();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        // Temporary in-memory user for testing
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("user")
                .password("password")
                .roles("USER")
                .build();
        return new MapReactiveUserDetailsService(user);
    }
}
