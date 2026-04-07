package com.hometalk.onepass.entity.message;

import lombok.Builder;

import java.time.LocalDateTime;

public class Message {

    private Long id;
    private Long userId;
    private String email;
    private String name;
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
        this.isSaveMsg = true;
        this.isSend = true;
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
