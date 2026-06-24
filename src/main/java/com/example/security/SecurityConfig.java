package com.example.security;

import com.example.model.User;
import com.example.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı."));

            if (!user.isActive()) {
                throw new UsernameNotFoundException("Bu hesap pasif durumda.");
            }

            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getEmail())
                    .password(user.getPassword())
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                    .build();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

 @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/**").permitAll()
        )
   .formLogin(form -> form
    .loginPage("/login")
    .loginProcessingUrl("/login")
    .usernameParameter("username")
    .passwordParameter("password")
    .permitAll()
)
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login")
            .permitAll()
        );

    return http.build();
}}