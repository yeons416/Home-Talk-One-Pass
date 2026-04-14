package com.hometalk.onepass.dashboard.dto.notification;

import com.hometalk.onepass.auth.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationToParkingDto {

    private Long id;
    private User user;                  // 등록한 회원 ID FK
    private String moduleName;          // 알림 발생 모듈
    private String categoryAlarm;       // 모듈별 세부 분류
    private String message;             // 알림 내용 메시지
    private Long referenceId;           // 각 카테고리 참조 테이블
    private Boolean is_read;            // 읽음 여부 상태
    private LocalDateTime deletedAt;    // 삭제 시각
    private String vehicleNumber;      // 차량 번호 전체

    /* Entity --> DTO 변환 메서드 (정적 팩토리 메서드) */
    //public static NotificationToParkingDto from(Notification notification) {}
    //    public static BookResponseDto from(Book book) {
//        return BookResponseDto.builder()
//                .id(book.getId())
//                .title(book.getTitle())
//                .author(book.getAuthor())
//                .price(book.getPrice())
//                .page(book.getPage())
//                .build();
//    }

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "module_name", nullable = false, length = 50)
//    private String moduleName;         // 알림 발생 모듈
//
//    @ManyToOne
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;          // 등록한 회원 ID FK

//    @Column(name = "category_alarm", nullable = false, length = 50)
//    private String categoryAlarm;      // 모듈별 세부 분류
//
//    @Column(nullable = false, length = 500)
//    private String message;             // 알림 내용 메시지
//
//    @Column(name = "reference_id")
//    private Long referenceId;
//
//    private Boolean is_read;            // 읽음 여부 상태
//
//    @Column(nullable = true)
//    private LocalDateTime deletedAt;            // 삭제 시각
//
//    // 주차 알림
//    @Column(name = "vehicle_number", nullable = false, unique = true,length = 100)
//    private String vehicleNumber;      // 차량 번호 전체
//
//    // 관리비
//    @Column(name = "billing_month", nullable = false, unique = true, length = 50)
//    private String billingMonth;       // 청구월 (예: 2026-03)
//
//    @Column(name = "total_amount",nullable = false, precision = 12, scale = 0)
//    private BigDecimal totalAmount;     // 합계 금액

//    private Long id;
//    private String title;
//    private String author;
//    private Integer price;
//    private Integer page;
//
//    /* Entity --> DTO 변환 메서드 (정적 팩토리 메서드) */
//    public static BookResponseDto from(Book book) {
//        return BookResponseDto.builder()
//                .id(book.getId())
//                .title(book.getTitle())
//                .author(book.getAuthor())
//                .price(book.getPrice())
//                .page(book.getPage())
//                .build();
//    }
}
