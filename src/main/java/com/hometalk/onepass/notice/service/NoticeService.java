package com.hometalk.onepass.notice.service;


import com.hometalk.onepass.notice.dto.NoticeDetailResponseDto;
import com.hometalk.onepass.notice.dto.NoticeListResponseDto;
import com.hometalk.onepass.notice.dto.NoticeRequestDto;
import com.hometalk.onepass.notice.entity.Attachment;
import com.hometalk.onepass.notice.entity.Badge;
import com.hometalk.onepass.notice.entity.Notice;
import com.hometalk.onepass.notice.repository.AttachmentRepository;
import com.hometalk.onepass.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final AttachmentRepository attachmentRepository;

    // 공지 전체 목록 조회 , 상단 고정, 최신순 정렬
    public List<NoticeListResponseDto> getNoticeList() {

        List<Notice> notices = noticeRepository.findAllByOrderByIsPinnedDescCreatedAtDesc();

        return notices.stream().map(notice -> new NoticeListResponseDto(
                        notice.getId(),
                        notice.getTitle(),
                        notice.getBadge(),
                        notice.getIsPinned(),
                        notice.getViewCount(),
                        notice.getCreatedAt(),
                        notice.getUpdatedAt()
                ))
                .collect(Collectors.toList());
    }

    // 공지 작성
    public Long createNotice(NoticeRequestDto noticeRequestDto) {
        Notice notice = new Notice();
        notice.setTitle(noticeRequestDto.getTitle());
        notice.setContent(noticeRequestDto.getContent());
        notice.setIsPinned(noticeRequestDto.getIsPinned());
        notice.setBadge(noticeRequestDto.getBadge());

        return noticeRepository.save(notice).getId();
    }

    // 공지 수정
    public Long updateNotice(Long id, NoticeRequestDto noticeRequestDto) {
            Notice notice = noticeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("공지를 찾을 수 없습니다."));

            notice.setTitle(noticeRequestDto.getTitle());
            notice.setContent(noticeRequestDto.getContent());
            notice.setIsPinned(noticeRequestDto.getIsPinned());
            notice.setBadge(noticeRequestDto.getBadge());

            return notice.getId();
    }

    //공지 삭제
    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지를 찾을 수 없습니다."));

        noticeRepository.delete(notice);
    }

    // 공지 상세 조회 (조회수)
    public NoticeDetailResponseDto getNoticeDetail(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지를 찾을 수 없습니다."));

        notice.setViewCount(notice.getViewCount() + 1);

        return new NoticeDetailResponseDto(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getViewCount(),
                notice.getBadge(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }

    // 이전글
    public NoticeListResponseDto getPreNotice(Long id) {
        return noticeRepository.findFirstByIdLessThanOrderByIdDesc(id)
                .map(notice -> new NoticeListResponseDto(
                        notice.getId(),
                        notice.getTitle(),
                        notice.getBadge(),
                        notice.getIsPinned(),
                        notice.getViewCount(),
                        notice.getCreatedAt(),
                        notice.getUpdatedAt()
                ))
        .orElse(null);
    }

    // 다음글
    public NoticeListResponseDto getNextNotice(Long id) {
        return noticeRepository.findFirstByIdGreaterThanOrderByIdAsc(id)
                .map(notice -> new NoticeListResponseDto(
                        notice.getId(),
                        notice.getTitle(),
                        notice.getBadge(),
                        notice.getIsPinned(),
                        notice.getViewCount(),
                        notice.getCreatedAt(),
                        notice.getUpdatedAt()
                ))
                .orElse(null);
    }

    // 제목, 내용 키워드 검색
    public List<NoticeListResponseDto> searchNotice(String keyword) {
        // keyword를 제목과 내용 둘 다 검색
        List<Notice> notices = noticeRepository.findByTitleContainingOrContentContaining(keyword, keyword);

        return notices.stream().map(notice -> new NoticeListResponseDto(
                notice.getId(),
                notice.getTitle(),
                notice.getBadge(),
                notice.getIsPinned(),
                notice.getViewCount(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        ))
                .collect(Collectors.toList());
    }
    // 첨부파일
    public List<Attachment> getAttachments(Long noticeId) {

        return attachmentRepository.findByNoticeId(noticeId);
    }



}
