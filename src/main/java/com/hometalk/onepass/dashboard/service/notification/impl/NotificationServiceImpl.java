package com.hometalk.onepass.dashboard.service.notification.impl;


import com.hometalk.onepass.dashboard.entity.notification.response.NotificationResponseDto;
import com.hometalk.onepass.dashboard.repository.notification.NotificationRepository;
import com.hometalk.onepass.dashboard.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{

    @Override
    public List<NotificationResponseDto> findByIsReadFalseOrderByCreatedAtDesc() {

        return null;
//        // false를 전달하여 '읽지 않은' 알림 조회
//        return NotificationRepository.findByIsReadOrderByCreatedAtDesc(false)
//                .stream()
//                .map(NotificationResponseDto::from)
//                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponseDto> findByIsReadTrueOrderByCreatedAtDesc() {

        return null;
//        // true를 전달하여 '읽은' 알림 조회
//        return NotificationRepository.findByIsReadOrderByCreatedAtDesc(true)
//                .stream()
//                .map(NotificationResponseDto::from)
//                .collect(Collectors.toList());
    }
}
