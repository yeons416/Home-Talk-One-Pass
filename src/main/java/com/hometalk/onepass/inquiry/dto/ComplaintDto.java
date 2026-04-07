package com.hometalk.onepass.inquiry.dto;
import com.hometalk.onepass.inquiry.entity.Complaint;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintDto {

    private Long userId;
    private String title;
    private String category;
    private String content;
    private boolean isSecret;
    private int viewCount;
    private String status;
    private String answer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ComplaintDto fromEntity(Complaint complaint) {
        return ComplaintDto.builder()
                .userId(complaint.getUser().getId())    // [수정] 소문자 객체명 사용
                .title(complaint.getTitle())            // [수정] 소문자 사용 및 () 추가
                .category(complaint.getCategory())
                .content(complaint.getContent())
                .isSecret(complaint.isSecret())
                .viewCount(complaint.getViewCount())
                .answer(complaint.getAnswer())
                .status(complaint.getStatus())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .build();
    }
}
