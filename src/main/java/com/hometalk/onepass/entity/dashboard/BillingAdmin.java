package com.hometalk.onepass.entity.dashboard;

import lombok.Builder;

public class BillingAdmin {

    private Long userId;
    private STATUS status;
    private String name;
    private String nickname;
    private String dong;
    private String ho;

    @Builder
    public BillingAdmin(Long userId, STATUS status, String name, String nickname, String dong, String ho) {
        this.userId = userId;
        this.status = status;
        this.name = name;
        this.nickname = nickname;
        this.dong = dong;
        this.ho = ho;
    }
}
