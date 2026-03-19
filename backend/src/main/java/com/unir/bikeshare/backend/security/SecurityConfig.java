package com.unir.bikeshare.backend.security;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomUserDetailsService userDetailsService;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/auth/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html"

            ).permitAll()
            .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
            .requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated()
            .requestMatchers(HttpMethod.DELETE, "/api/users/me").authenticated()
            .requestMatchers("/api/users/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/api/stations/**").hasAnyRole("RIDER", "ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/stations/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/api/stations/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/stations/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/bikes/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/api/bikes/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/bikes/**").hasRole("ADMIN")
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/**").hasAnyRole("RIDER", "ADMIN")
            .anyRequest().permitAll())
        .userDetailsService(userDetailsService)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .formLogin(form -> form.disable())
        .httpBasic(httpBasic -> httpBasic.disable());

    return http.build();
  }

  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:4200"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
    config.setExposedHeaders(List.of("Authorization"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
