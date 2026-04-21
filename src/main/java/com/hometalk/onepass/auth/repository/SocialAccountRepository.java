package com.hometalk.onepass.auth.repository;

import com.hometalk.onepass.auth.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository <SocialAccount, Long> {
    // 플랫폼(kakao)과 해당 플랫폼의 이메일로 연동된 계정이 있는지 확인
    Optional<SocialAccount> findByPlatformAndPlatformId(SocialAccount.Platform platform, String platformId);
}
