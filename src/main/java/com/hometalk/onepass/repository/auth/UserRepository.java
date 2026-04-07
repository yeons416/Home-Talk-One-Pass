package com.hometalk.onepass.repository.auth;

import com.hometalk.onepass.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
