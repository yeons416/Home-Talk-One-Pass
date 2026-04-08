package com.hometalk.onepass.auth.repository;

import com.hometalk.onepass.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
