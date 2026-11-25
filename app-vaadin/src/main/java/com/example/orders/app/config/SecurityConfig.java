package com.example.orders.app.config;

import com.example.orders.app.ui.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** REST-API: zustandslos, HTTP Basic. Produkte ändern darf nur ADMIN. */
    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .requestMatchers("/api/products/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    /** Vaadin-UI: Formular-Login, Zugriff per @PermitAll / @RolesAllowed auf den Views. */
    @Bean
    @Order(2)
    SecurityFilterChain uiSecurity(HttpSecurity http) throws Exception {
        // "/error" freigeben, sonst wird ein 401/403 der API beim Error-Dispatch zum Login-Redirect (302)
        http.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/error").permitAll());
        http.with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin.loginView(LoginView.class));
        return http.build();
    }

    /** Nur für die Demo! In echt: LDAP, OIDC (Keycloak, Entra ID ...) oder eine Benutzer-DB. */
    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password("{noop}admin").roles("ADMIN", "USER").build(),
                User.withUsername("user").password("{noop}user").roles("USER").build());
    }
}
