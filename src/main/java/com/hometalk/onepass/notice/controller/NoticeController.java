package com.hometalk.onepass.notice.controller;

import com.hometalk.onepass.notice.dto.NoticeDetailResponseDto;
import com.hometalk.onepass.notice.dto.NoticeListResponseDto;
import com.hometalk.onepass.notice.dto.NoticeRequestDto;
import com.hometalk.onepass.notice.entity.Attachment;
import com.hometalk.onepass.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    // 목록
    @GetMapping
    public String noticeList(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(required = false) String keyword,
                             Model model) {
        Page<NoticeListResponseDto> notices;

        if (keyword == null || keyword.trim().isEmpty()) {
            notices = noticeService.getNoticeList(page);
        } else {
            notices = noticeService.searchNotice(keyword, page);
        }

        model.addAttribute("notices", notices);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notices.getTotalPages());
        model.addAttribute("keyword", keyword);
        return "notice/noticeList";
    }

    // 상세
    @GetMapping("/{id}")
    public String noticeDetail(@PathVariable Long id, Model model) {
        NoticeDetailResponseDto notice = noticeService.getNoticeDetail(id);
        NoticeListResponseDto preNotice = noticeService.getPreNotice(id);
        NoticeListResponseDto nextNotice = noticeService.getNextNotice(id);
        List<Attachment> attachments = noticeService.getAttachments(id); // 추가

        model.addAttribute("notice", notice);
        model.addAttribute("preNotice", preNotice);
        model.addAttribute("nextNotice", nextNotice);
        model.addAttribute("attachments", attachments); // 추가
        return "notice/noticeDetail";
    }

    // 작성 페이지
    @GetMapping("/write")
    public String noticeWriteForm() {
        return "notice/noticeForm";
    }

    // 작성 처리
    @PostMapping("/write")
    public String noticeWrite(@ModelAttribute NoticeRequestDto noticeRequestDto,
                              @RequestParam(required = false) MultipartFile file) {
        Long id = noticeService.createNotice(noticeRequestDto, file);
        return "redirect:/notice/" + id;
    }

    // 수정 페이지
    @GetMapping("/{id}/edit")
    public String noticeEditForm(@PathVariable Long id, Model model) {
        NoticeDetailResponseDto notice = noticeService.getNoticeForEdit(id);
        model.addAttribute("notice", notice);
        return "notice/noticeEdit";
    }

    // 수정 처리
    @PostMapping("/{id}/edit")
    public String noticeEdit(@PathVariable Long id,
                             @ModelAttribute NoticeRequestDto noticeRequestDto,
                             @RequestParam(required = false) MultipartFile file) { // 추가
        noticeService.updateNotice(id, noticeRequestDto, file); // file 추가
        return "redirect:/notice/" + id;
    }

    // 삭제
    @PostMapping("/{id}/delete")
    public String noticeDelete(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return "redirect:/notice";
    }

    // 파일 다운로드
    @GetMapping("/download/{attachmentId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long attachmentId) {
        Attachment attachment = noticeService.getAttachment(attachmentId);

        Path path = Paths.get(attachment.getFilePath());
        Resource resource = new FileSystemResource(path);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
    }
}