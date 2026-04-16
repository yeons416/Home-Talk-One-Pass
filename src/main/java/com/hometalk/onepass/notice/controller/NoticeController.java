package com.hometalk.onepass.notice.controller;

import com.hometalk.onepass.notice.dto.NoticeDetailResponseDto;
import com.hometalk.onepass.notice.dto.NoticeListResponseDto;
import com.hometalk.onepass.notice.dto.NoticeRequestDto;
import com.hometalk.onepass.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

        model.addAttribute("notice", notice);
        model.addAttribute("preNotice", preNotice);
        model.addAttribute("nextNotice", nextNotice);
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
        NoticeDetailResponseDto notice = noticeService.getNotice(id);
        model.addAttribute("notice", notice);
        return "notice/noticeEdit";
    }

    // 수정 처리
    @PostMapping("/{id}/edit")
    public String noticeEdit(@PathVariable Long id,
                             @ModelAttribute NoticeRequestDto noticeRequestDto) {
        noticeService.updateNotice(id, noticeRequestDto);
        return "redirect:/notice/" + id;
    }

    // 삭제
    @PostMapping("/{id}/delete")
    public String noticeDelete(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return "redirect:/notice";
    }
}