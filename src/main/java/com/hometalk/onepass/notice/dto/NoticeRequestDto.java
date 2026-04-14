package com.hometalk.onepass.notice.dto;

import com.hometalk.onepass.notice.entity.Badge;
import com.hometalk.onepass.notice.entity.Notice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NoticeRequestDto {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private Boolean isPinned;

    @NotNull
    private Badge badge;

}