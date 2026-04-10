package com.hometalk.onepass.dashboard.entity.message;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Builder;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

public class Message {

    @Id // 1. 필수 추가
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 2. 자동 생성 시 추가
    private Long id;

    private Long userId;
    private String email;
    private String name;

    @Enumerated(EnumType.STRING) // Enum 타입인 경우 추가 권장
    private CATEGORY categoryMsg;

    private String moduleName;
    private String messageSubTitle;
    private String messageDetail;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private Boolean isSaveMsg;
    private Boolean isSend;

    @Builder
    public Message(Long id, Long userId, String email, String name, String moduleName, String messageSubTitle, String messageDetail,
                   LocalDateTime createdAt, LocalDateTime deletedAt, Boolean isSaveMsg, Boolean isSend) {
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.moduleName = moduleName;
        this.messageSubTitle = messageSubTitle;
        this.messageDetail = messageDetail;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
        this.isSaveMsg = isSaveMsg != null ? isSaveMsg : true;
        this.isSend = isSend != null ? isSend : true;
        this.isRead = false;
    }

    public void setIsRead() {
        this.isRead = true;
    }

    public void setIsSaveMsg(boolean isSaveMsg) {
        this.isSaveMsg = isSaveMsg;
    }

    public void setIsSend(boolean isSend) {
        this.isSend = isSend;
    }
}
