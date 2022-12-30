package com.caster.redis.schedule;

import com.caster.redis.constan.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 修剪Stream
 *
 * Author: caster
 * Date: 2022/12/30
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class XTrimStreamTask {
    private final RedisTemplate<String, Object> redisTemplate;

//    @Scheduled(cron ="0 0 0 * * ?")
    @Scheduled(cron ="0 * * * * ?")
    public void execute() {
        // 修剪stream 只留下最後15筆資料  Ex. totalMessage:45 -> trim(15) -> removeCount=30
        Long removeCount = redisTemplate.opsForStream().trim(Constants.STREAM_KEY_001, 15l);
        log.info("XTrimStreamTask > 修剪Stream removeCount:{}", removeCount);
    }
}
