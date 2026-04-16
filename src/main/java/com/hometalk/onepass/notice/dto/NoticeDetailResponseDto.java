package com.hometalk.onepass.notice.dto;


import com.hometalk.onepass.notice.entity.Badge;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NoticeDetailResponseDto {

    private Long id;

    private String title;

    private String content;

    private int viewCount;

    private Badge badge;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}