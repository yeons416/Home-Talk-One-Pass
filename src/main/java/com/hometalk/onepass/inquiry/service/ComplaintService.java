package com.hometalk.onepass.inquiry.service;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.inquiry.dto.ComplaintDto;
import com.hometalk.onepass.inquiry.entity.Complaint;
import com.hometalk.onepass.inquiry.repository.ComplaintRepository;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    /*
        민원 등록
     */
    @Transactional
    public Long register(ComplaintDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("작성 유저를 찾을 수 없습니다. ID: " + dto.getUserId()));
        Complaint complaint = Complaint.builder()
                .user(user)
                .title(dto.getTitle())
                .category(dto.getCategory())
                .content(dto.getContent())
                .isSecret(dto.isSecret())
                .status("접수완료") // 초기 상태값
                .build();
        return complaintRepository.save(complaint).getId();
    }
    /*
        전체 민원 조회
     */
    public List<ComplaintDto> findAll() {
        return complaintRepository.findAll().stream()
                .map(ComplaintDto::fromEntity)
                .toList();
    }
    /*
        특정 민원 상세 조회
     */
    public Complaint findOne(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 민원을 찾을 수 없습니다."));
    }

    /*
         관리자 답변 등록 (상태 변경 포함)
     */
    @Transactional
    public void respond(Long id, String response) {
        Complaint complaint = findOne(id);
        complaint.addResponse(response);
    }

    /*
        민원 삭제
     */
    @Transactional
    public void delete(Long id) {
        complaintRepository.deleteById(id);
    }
}
