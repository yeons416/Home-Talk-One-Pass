package com.hometalk.onepass.inquiry.controller;

import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.entity.Inquiry;
import com.hometalk.onepass.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    /*
     * 민원 등록    (POST)
     * 입주민이 새로운 문의나 민원을 넣을 때 사용
     *  주소: POST http://localhost:8090/api/inquiries
     */
    @PostMapping
    public Long register(@RequestBody InquiryDto inquiryDto) {
        return inquiryService.register(inquiryDto);
    }

    /*
        전체 민원 목록 조회 (GET)
        관리자나 본인이 작성한 리스트를 볼 때 사용
     */
    @GetMapping
    public List<InquiryDto> list() {
        return inquiryService.findAll();
    }

    /*
        상세 조회 (GET)

     */
    @GetMapping("/{id}")
    public InquiryDto detail(@PathVariable("id") Long id) { // 괄호 오타 수정
        Inquiry inquiry = inquiryService.findOne(id);
        return InquiryDto.fromEntity(inquiry);
    }

    /*
        삭제 (DELETE)

     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        inquiryService.deleteInquiry(id);
    }
}
