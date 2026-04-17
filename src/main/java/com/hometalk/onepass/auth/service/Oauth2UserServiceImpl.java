package com.hometalk.onepass.auth.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class Oauth2UserServiceImpl extends DefaultOAuth2UserService {
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 카카오에서 준 유저 정보 (Map 형태)
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 여기서 DB 조회 및 회원가입 로직 처리
        // 예: String email = (String) attributes.get("account_email");

        return oAuth2User;
    }
}