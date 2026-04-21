package com.hometalk.onepass.auth.config;

import com.hometalk.onepass.auth.entity.SocialAccount;
import com.hometalk.onepass.auth.repository.SocialAccountRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final SocialAccountRepository socialAccountRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        SocialAccount.Platform platform = SocialAccount.Platform.valueOf(registrationId.toUpperCase());

        String email = "";
        String rawId = ""; // 소셜 제공 고유 ID

        // 1. 데이터 추출
        if (platform == SocialAccount.Platform.KAKAO) {
            rawId = String.valueOf(oAuth2User.getAttributes().get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
            email = (String) kakaoAccount.get("email");
        }
        // ... 네이버, 구글 로직 동일 ...

        // 2. 중요: DB 저장 규칙과 동일하게 platformId 가공 (email_PLATFORM)
        String combinedPlatformId = email + "_" + registrationId.toUpperCase();
        log.info("조회하려는 Platform: {}, PlatformId: {}", platform, combinedPlatformId);

        // 3. 가공된 ID로 DB 조회
        Optional<SocialAccount> socialAccount = socialAccountRepository.findByPlatformAndPlatformId(platform, combinedPlatformId);
        log.info("DB 존재 여부: {}", socialAccount.isPresent());

        if (socialAccount.isEmpty()) {
            String redirectUrl = String.format("/auth/register/social?email=%s&platform=%s&platformId=%s",
                    email, platform, combinedPlatformId);
            log.info("신규 유저 -> 가입 페이지로 리다이렉트: {}", redirectUrl);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return; // 더 이상 아래 코드가 실행되지 않도록 종료
        } else {
            log.info("기존 유저 -> 메인 페이지로 이동");
            getRedirectStrategy().sendRedirect(request, response, "/index");
            return;
        }
    }
}