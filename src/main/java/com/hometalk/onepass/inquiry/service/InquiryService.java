package com.hometalk.onepass.inquiry.service;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.entity.Inquiry;
import com.hometalk.onepass.inquiry.repository.InquiryRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    /*
     * 민원 등록
     * 입주민이 새로운 문의나 민원을 넣을 때 사용
     */
    @Transactional
    public Long register(InquiryDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("민원 작성 유저를 찾을 수 없습니다. ID: " + dto.getUserId()));

        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .title(dto.getTitle())
                .category(dto.getCategory())
                .content(dto.getContent())
                .build();

        return inquiryRepository.save(inquiry).getId();
    }
    /*
        전체 민원 목록 조회
        관리자나 본인이 작성한 리스트를 볼 때 사용
     */
    public List<InquiryDto> findAll() {
        return inquiryRepository.findAll().stream()
                .map(InquiryDto::fromEntity)
                .toList();
    }
    /*
        특정 민원 상세 조회
        민원 내용을 클릭해서 자세히 볼 때 사용
     */
    public Inquiry findOne(Long id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 민원글 입니다."));
    }
    /*
        민원 삭제
     */
    @Transactional
    public void deleteInquiry(Long id) {
        inquiryRepository.deleteById(id);
    }
}
