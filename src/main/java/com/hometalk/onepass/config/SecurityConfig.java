package com.hometalk.onepass.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 1. "/" 경로와 정적 리소스(css, js 등)는 모두에게 허용
                        .requestMatchers("/", "/auth", "/auth/signup", "/auth/login", "/auth/register/**").permitAll()

                        // 2. 그 외의 모든 요청은 인증(로그인)이 필요함
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth")            // 1. 사용자 정의 로그인 페이지 경로
                        .loginProcessingUrl("/auth/login") // 2. 로그인 처리 URL
                        .defaultSuccessUrl("/index", true)   // 3. 로그인 성공 시 이동할 경로
                        .failureUrl("/auth?error=true")   // 4. 로그인 실패 시 이동할 경로
                        .permitAll()                  // 5. 로그인 페이지는 누구나 접근 가능해야 함
                        .usernameParameter("loginId") // username이 아닌 login_id으로 name 설정
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/auth") // 로그아웃 성공 시 이동할 페이지
                        .permitAll()
                );

        return http.build();

    }
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
