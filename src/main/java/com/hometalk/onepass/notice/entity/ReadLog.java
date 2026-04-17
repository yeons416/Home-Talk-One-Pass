package com.hometalk.onepass.notice.entity;


import com.hometalk.onepass.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "read_log",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "notice_id"}))
public class ReadLog extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long noticeId;
}