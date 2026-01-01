package com.advent.backend.service;

import com.advent.backend.repository.MemberRepository;
import com.advent.backend.repository.NotificationRepository;
import com.advent.backend.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {
    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final PointHistoryRepository pointHistoryRepository;
}
