package com.nhom4.xoxo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom4.xoxo.constant.WrapResStatus;
import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.security.JwtAuthenticationFilter;
import com.nhom4.xoxo.security.OAuth2SuccessHandler;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public OAuth2SuccessHandler oAuth2SuccessHandler() {
        return new OAuth2SuccessHandler();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            WrapRes<?> wrapRes = WrapRes.error(WrapResStatus.UNAUTHORIZED,
                                    "Unauthorized: User not authenticated");
                            ObjectMapper objectMapper = new ObjectMapper();
                            response.getWriter().write(objectMapper.writeValueAsString(wrapRes));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            WrapRes<?> wrapRes = WrapRes.error(WrapResStatus.SECURITY_ERROR, "Access denied");
                            ObjectMapper objectMapper = new ObjectMapper();
                            response.getWriter().write(objectMapper.writeValueAsString(wrapRes));
                        }))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "OWNER")
                        .requestMatchers("/api/owner/**").hasRole("OWNER")
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN", "OWNER")
                        .requestMatchers("/api/posts/**").hasAnyRole("USER", "ADMIN", "OWNER")
                        .requestMatchers("/api/v1/reports/**").hasAnyRole("USER", "ADMIN", "OWNER")
                        .requestMatchers("/api/v1/groups/**").hasAnyRole("USER", "ADMIN", "OWNER")
                        .requestMatchers("/api/v1/group-members/**").hasAnyRole("USER", "ADMIN", "OWNER")
                        .requestMatchers("/api/friendships/**").hasAnyRole("USER", "ADMIN", "OWNER")
                        .requestMatchers("/api/newsfeed/**").hasAnyRole("USER", "ADMIN", "OWNER")
                        .requestMatchers("/login", "/oauth2/**").permitAll()
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/oauth2/success")
                        .successHandler(oAuth2SuccessHandler())
                        .failureUrl("/oauth2/error"))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .invalidateHttpSession(true)
                        .deleteCookies("refreshToken"))
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}