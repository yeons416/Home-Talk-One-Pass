package com.hometalk.onepass.community.repository;

import com.hometalk.onepass.community.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
