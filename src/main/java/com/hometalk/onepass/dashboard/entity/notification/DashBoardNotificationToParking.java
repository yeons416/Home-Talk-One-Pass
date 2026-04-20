package com.hometalk.onepass.dashboard.entity.notification;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)      // JPA 스펙 상 기본 생성자 필수, PROTECTED로 외부 직접 생성 차단함.
@AllArgsConstructor(access = AccessLevel.PRIVATE)       // @Builder 내부 동작용 전체 생성자, PRIVATE으로 외부 노출 차단함.
@Builder  // id를 제외하고 필요한 필드만 선택적으로 주입 가능함. 예: Book.builder().title("AI의 미래").price(30000).build()
@Entity
@Table(name = "DashBoardNotificationToParking")  // 테이블명 명시. 생략 시 클래스명 기반 자동 지정됨. 테이블명은 복수형 사용.
public class DashBoardNotificationToParking extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_name", nullable = false, length = 50)
    private String moduleName;         // 알림 발생 모듈

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;          // 등록한 회원 ID FK

    @Column(name = "category_alarm", nullable = false, length = 50)
    private String categoryAlarm;      // 모듈별 세부 분류

    @Column(nullable = false, length = 500)
    private String message;             // 알림 내용 메시지

    @Column(name = "reference_id")
    private Long referenceId;

    private Boolean is_read;            // 읽음 여부 상태

    @Column(nullable = true)
    private LocalDateTime deletedAt;            // 삭제 시각

    // 주차 알림
    @Column(name = "vehicle_number", nullable = false, unique = true,length = 100)
    private String vehicleNumber;      // 차량 번호 전체

    public DashBoardNotificationToParking(Long id, Long user_id, String module_name, String category_alarm, String message,
                                          Long reference_id, Boolean is_read, String vehicle_number) {
        this.id = id;
        this.moduleName = module_name;
        this.categoryAlarm = category_alarm;
        this.message = message;
        this.referenceId = reference_id;
        this.is_read = is_read;
        this.vehicleNumber = vehicle_number;
    }

//    private Long user_id;               // 등록한 회원 ID FK
//    private String module_name;         // 알림 발생 모듈
//    private String category_alarm;      // 모듈별 세부 분류
//    private String message;             // 알림 내용 메시지
//    private Long reference_id;          // 대상 모듈의 PK
//    private Boolean is_read;            // 읽음 여부 상태
//    private LocalDateTime createdAt;            // 알림 생성 시각
//    private LocalDateTime deletedAt;            // 삭제 시각
//
//    // 주차 알림
//    private String vehicle_number;      // 차량 번호 전체
//
//    // 관리비
//    private String billing_month;       // 청구월 (예: 2026-03)
//    private int total_amount;           // 합계 금액
//    private boolean status;             // UNPAID / PAID (관리비 납부 유무)
//    private LocalDateTime due_date;     // 납기일
//    private List<String> item_name;           // 전기료, 수도료, 청소비 등 항목명
//    private List<Integer> item_amount;            // 개별 항목 금액

//    @Builder
//    public Notification(Long id, Long user_id, String module_name, String category_alarm, String message,
//                        Long reference_id, Boolean is_read, LocalDateTime createdAt) {
//
//        this.id = id;
//        this.user_id = user_id;
//        this.module_name = module_name;
//        this.category_alarm = category_alarm;
//        this.message = message;
//        this.reference_id = reference_id;
//        this.is_read = is_read;
//        this.createdAt = createdAt;
//        this.deletedAt = null;
//    }
//
//    // 주차 알림
//    public void alarmToParking(String vehicle_number) {
//
//        this.vehicle_number = vehicle_number;
//    }

//    // 관리비 알림
//    public void alarmToBilling(String billing_month, int total_amount, boolean status, LocalDateTime due_date,
//                               List<String> item_name, List<Integer> item_amount) {
//        this.billing_month = billing_month;
//        this.total_amount = total_amount;
//        this.status = status;
//        this.due_date = due_date;
//        this.item_name = item_name;
//        this.item_amount = item_amount;
//    }
}
