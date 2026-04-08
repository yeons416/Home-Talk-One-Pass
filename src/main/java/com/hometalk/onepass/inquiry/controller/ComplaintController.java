package com.hometalk.onepass.inquiry.controller;

import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.inquiry.dto.ComplaintDto;
import com.hometalk.onepass.inquiry.entity.Complaint;
import com.hometalk.onepass.inquiry.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UserRepository userRepository;

    // 민원 등록
    @PostMapping
    public Long register(@RequestBody ComplaintDto dto) {

        return complaintService.register(dto);
    }

    // 전체 민원 조회
    @GetMapping
    public List<ComplaintDto> findAll() {
        return complaintService.findAll();
    }

    // 상세 민원 조회
    @GetMapping("/{id}")
    public ComplaintDto findOne(@PathVariable Long id) {
        Complaint complaint = complaintService.findOne(id);
        // 미리 만들어둔 fromEntity 메서드를 사용해서 변환해줍니다!
        return ComplaintDto.fromEntity(complaint);
    }

    // 관리자 답변 등록
    @PostMapping("/{id}")
    public void respond(@PathVariable Long id, @RequestBody String response) {
        complaintService.respond(id, response);
    }

    // 민원 삭제
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        complaintService.delete(id);
    }
}
