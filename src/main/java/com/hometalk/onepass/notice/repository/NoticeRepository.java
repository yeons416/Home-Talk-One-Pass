package com.hometalk.onepass.notice.repository;

import com.hometalk.onepass.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 이전글, 다음글
    Optional<Notice> findFirstByIdLessThanOrderByIdDesc(Long id);
    Optional<Notice> findFirstByIdGreaterThanOrderByIdAsc(Long id);

    // 제목, 내용 키워드 검색 + 페이지네이션
    Page<Notice> findByTitleContainingOrContentContaining(String title, String content, Pageable pageable);
}