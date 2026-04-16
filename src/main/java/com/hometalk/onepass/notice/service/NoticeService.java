package com.hometalk.onepass.notice.service;

import com.hometalk.onepass.notice.dto.NoticeDetailResponseDto;
import com.hometalk.onepass.notice.dto.NoticeListResponseDto;
import com.hometalk.onepass.notice.dto.NoticeRequestDto;
import com.hometalk.onepass.notice.entity.Attachment;
import com.hometalk.onepass.notice.entity.Notice;
import com.hometalk.onepass.notice.exception.NoticeNotFoundException;
import com.hometalk.onepass.notice.repository.AttachmentRepository;
import com.hometalk.onepass.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final AttachmentRepository attachmentRepository;

    @Value("${file.upload.path}")
    private String uploadPath;

    // 공지 전체 목록 조회, 상단 고정, 최신순 정렬
    public Page<NoticeListResponseDto> getNoticeList(int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("isPinned").descending().and(Sort.by("createdAt").descending()));
        Page<Notice> notices = noticeRepository.findAll(pageable);
        return notices.map(notice -> new NoticeListResponseDto(
                notice.getId(),
                notice.getTitle(),
                notice.getBadge(),
                notice.getIsPinned(),
                notice.getViewCount(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        ));
    }

    // 공지 작성 (파일 포함)
    public Long createNotice(NoticeRequestDto noticeRequestDto, MultipartFile file) {

        if (noticeRequestDto.getBadge() == null) {
            throw new IllegalArgumentException("분류를 선택해주세요.");
        }

        Notice notice = new Notice();
        notice.setTitle(noticeRequestDto.getTitle());
        notice.setContent(noticeRequestDto.getContent());
        notice.setIsPinned(Boolean.TRUE.equals(noticeRequestDto.getIsPinned()));
        notice.setBadge(noticeRequestDto.getBadge());
        noticeRepository.save(notice);

        if (file != null && !file.isEmpty()) {
            saveFile(file, notice);
        }
        return notice.getId();
    }

    // 파일 저장
    private void saveFile(MultipartFile file, Notice notice) {
        try {
            File dir = new File(uploadPath);
            if (!dir.exists()) dir.mkdirs();

            String original = file.getOriginalFilename();
            String fileName = UUID.randomUUID() + "_" + (original != null ? original : "file");
            String filePath = uploadPath + "/" + fileName;

            file.transferTo(new File(filePath).getAbsoluteFile());

            Attachment attachment = new Attachment();
            attachment.setNotice(notice);
            attachment.setFileName(fileName);
            attachment.setFilePath(filePath);
            attachment.setFileSize(file.getSize());
            attachmentRepository.save(attachment);

        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + e.getMessage(), e);
        }
    }

    // 공지 수정
    public Long updateNotice(Long id, NoticeRequestDto noticeRequestDto) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new NoticeNotFoundException(id));

        if (noticeRequestDto.getBadge() == null) {
            throw new IllegalArgumentException("분류를 선택해주세요.");
        }

        notice.setTitle(noticeRequestDto.getTitle());
        notice.setContent(noticeRequestDto.getContent());
        notice.setIsPinned(Boolean.TRUE.equals(noticeRequestDto.getIsPinned()));
        notice.setBadge(noticeRequestDto.getBadge());

        noticeRepository.save(notice);
        return notice.getId();
    }

    // 공지 삭제
    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new NoticeNotFoundException(id));

        List<Attachment> attachments = attachmentRepository.findByNotice(notice);
        for (Attachment attachment : attachments) {
            File file = new File(attachment.getFilePath());
            if (file.exists()) file.delete();
        }
        attachmentRepository.deleteByNotice(notice);
        noticeRepository.delete(notice);
    }

    // 공지 상세 조회 (조회수)
    public NoticeDetailResponseDto getNoticeDetail(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new NoticeNotFoundException(id));

        notice.setViewCount(notice.getViewCount() + 1);
        noticeRepository.save(notice);

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

    // 제목, 내용 키워드 검색 (최신순)
    public Page<NoticeListResponseDto> searchNotice(String keyword, int page) {
        Pageable pageable = PageRequest.of(page, 10,
                Sort.by("isPinned").descending().and(Sort.by("createdAt").descending()));
        Page<Notice> notices = noticeRepository.findByTitleContainingOrContentContaining(keyword, keyword, pageable);
        return notices.map(notice -> new NoticeListResponseDto(
                notice.getId(),
                notice.getTitle(),
                notice.getBadge(),
                notice.getIsPinned(),
                notice.getViewCount(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        ));
    }
    // 첨부파일 조회
    public List<Attachment> getAttachments(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException(noticeId));
        return attachmentRepository.findByNotice(notice);
    }

    // 수정 페이지용 조회 (조회수 증가X)
    public NoticeDetailResponseDto getNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new NoticeNotFoundException(id));

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
}