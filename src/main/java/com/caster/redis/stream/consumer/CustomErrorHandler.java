package com.caster.redis.stream.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ErrorHandler;

/**
 * StreamPollTask 獲取消息或對應的listener消費消息過程中發生了例外事件
 *
 * Author: Minchang Hsu (Caster)
 * Date: 2022/12/30
 */
@Slf4j
public class CustomErrorHandler implements ErrorHandler {
    @Override
    public void handleError(Throwable t) {
        log.error("Exception > ", t);
    }
}
