package com.hometalk.onepass.schedule.entity;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.common.entity.BaseTimeEntity;
import com.hometalk.onepass.notice.entity.Notice;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // 외부에서 직접 객체 생성 못 함 (같은 패키지나 상속받은 클래스에서만 접근 가능)
                                                    // → Builder로만 가능
public class Schedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id")
    private Notice notice;

    @Column(nullable = false, length = 100)
    private String title;

    private String info;
    private String location;
    private String referenceUrl;

    @Column(nullable = false)
    private LocalDateTime startAt; // 필수

    private LocalDateTime endAt;

    @Builder
    public Schedule(User user, Notice notice, String title, String info,
                    String location, String referenceUrl,
                    LocalDateTime startAt, LocalDateTime endAt) {
        validateTitle(title);
        validateTime(startAt, endAt);
        this.user = user;
        this.notice = notice;
        this.title = title;
        this.info = info;
        this.location = location;
        this.referenceUrl = referenceUrl;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    // 수정 메서드
    public void update(String title, String info, String location,
                       String referenceUrl, LocalDateTime startAt, LocalDateTime endAt) {
        validateTitle(title);
        validateTime(startAt, endAt);
        this.title = title;
        this.info = info;
        this.location = location;
        this.referenceUrl = referenceUrl;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목을 입력해주세요.");
        }
    }

    private void validateTime(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null) {
            throw new IllegalArgumentException("시작 시간을 입력해주세요.");
        }
        if (endAt != null && endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("종료 시간이 시작 시간보다 빠를 수 없습니다.");
        }
    }
}