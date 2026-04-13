package com.hometalk.onepass.auth.service;

import com.hometalk.onepass.auth.dto.SignUpDTO;
import com.hometalk.onepass.auth.entity.LocalAccount;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.LocalAccountRepository;
import com.hometalk.onepass.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignUpService {

    private final UserRepository userRepository;
    private final LocalAccountRepository localAccountRepository;
    private final BCryptPasswordEncoder bcryptPasswordEncoder;

    // 회원 가입 서비스
    @Transactional
    public void signUp(SignUpDTO dto) {
        // 1. User (부모) 생성
        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .phoneNumber(dto.getPhoneNumber())
                .build();

        User savedUser = userRepository.save(user);

        // 2. LocalAccount (자식) 생성
        LocalAccount localAccount = LocalAccount.builder()
                .user(savedUser) // @MapsId로 연결된 User 객체
                .loginId(dto.getLoginId())
                .passwordHash(bcryptPasswordEncoder.encode(dto.getPassword())) // 평문 저장 (테스트용)
                .build();

        localAccountRepository.save(localAccount);
    }
}