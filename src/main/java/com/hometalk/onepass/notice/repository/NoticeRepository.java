package com.hometalk.onepass.notice.repository;

import com.hometalk.onepass.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
}