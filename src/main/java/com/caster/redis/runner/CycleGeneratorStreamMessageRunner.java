package com.caster.redis.runner;

import com.caster.redis.stream.producer.StreamProducer;
import lombok.AllArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.caster.redis.constan.Constants.STREAM_KEY_001;

/**
 * 定期產生訊息塞入訊息佇列
 *
 * Author: Minchang Hsu (Caster)
 * Date: 2022/12/30
 */
@Component
@AllArgsConstructor
public class CycleGeneratorStreamMessageRunner implements ApplicationRunner {

    private final StreamProducer streamProducer;

    @Override
    public void run(ApplicationArguments args) {
        // 每五秒產生訊息塞入訊息佇列
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> streamProducer.sendRecord(STREAM_KEY_001),
                        0, 5, TimeUnit.SECONDS);
    }
}
