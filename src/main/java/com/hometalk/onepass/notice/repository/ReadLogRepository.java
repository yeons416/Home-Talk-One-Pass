package com.hometalk.onepass.notice.repository;

import com.hometalk.onepass.notice.entity.ReadLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadLogRepository extends JpaRepository<ReadLog, Long> {
}