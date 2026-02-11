package com.hawkins.xtreamjson.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests((requests) -> requests
                                                .requestMatchers("/admin/**", "/api/admin/**", "/providers/**",
                                                                "/resetDatabase",
                                                                "/createStreams")
                                                .hasRole("ADMIN")
                                                .requestMatchers("/stream.html", "/proxy/**", "/transcode/**",
                                                                "/css/**", "/js/**",
                                                                "/images/**", "/webjars/**", "/", "/login")
                                                .permitAll()
                                                .anyRequest().permitAll())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .permitAll())
                                .logout(logout -> logout.logoutSuccessUrl("/").permitAll());

                return http.build();
        }

    @Bean
    InMemoryUserDetailsManager userDetailsService() {
                UserDetails admin = User.builder()
                                .username("admin")
                                .password("$2a$05$cRtC6BGeZG/yS42B2J8yye4K.zfozPF3M4TfQ50eLtSeq/xWvz/1a")
                                .roles("ADMIN")
                                .build();
                return new InMemoryUserDetailsManager(admin);
        }

    @Bean
    PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
