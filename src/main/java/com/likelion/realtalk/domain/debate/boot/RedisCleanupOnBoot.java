package com.likelion.realtalk.domain.debate.boot;

import com.likelion.realtalk.domain.debate.service.RedisParticipantsCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component // ← 항상 실행 (프로퍼티 조건 없이)
@RequiredArgsConstructor
public class RedisCleanupOnBoot implements ApplicationRunner {

    private final RedisParticipantsCleanupService cleanup;

    @Override
    public void run(ApplicationArguments args) {
        long n = cleanup.cleanupAllParticipantsKeys();
        log.info("[Redis] Cleaned participant keys on boot: {} keys", n);
    }
}
