package com.mulmi.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// @Scheduled 메서드를 찾아 정해진 시각에 실행하게 켠다 (이게 없으면 @Scheduled는 아무 일도 하지 않는다)
// 지금 쓰는 곳 : 대여 노쇼(RentalNoShowScheduler)
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
