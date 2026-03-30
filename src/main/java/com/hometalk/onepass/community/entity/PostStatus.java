package com.hometalk.onepass.community.entity;

public enum PostStatus {
    ACTIVE("활성"),
    HIDDEN("숨김"),
    DRAFT("임시저장");

    private final String description;

    PostStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
