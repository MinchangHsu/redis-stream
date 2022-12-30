package com.caster.redis.stream.consumer.impl;

import com.caster.redis.constan.Constants;
import com.caster.redis.entity.Book;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 用XREAD讀取stream數據，可以獲取到Stream中所有的消息
 *
 * 可啟動多個執行緒, 執行消費動作, 如果都是使用同消費群組, 則並不會重複執行.
 *
 * Author: Minchang Hsu (Caster)
 * Date: 2022/12/30
 */
@Component
@Slf4j
public class NonBlockConsumer01 implements InitializingBean, DisposableBean {

    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private volatile boolean stop = false;

    @Override
    public void afterPropertiesSet() {

        // 初始化線程池
        threadPoolExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(), r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("nonblock-01");
            return thread;
        });

        StreamReadOptions streamReadOptions = StreamReadOptions.empty()
                // 如果沒有數據，則阻塞1s 阻塞時間需要小於`spring.redis.timeout`配置的時間
                .block(Duration.ofMillis(1000))
                // 一直阻塞直到獲取數據，可能會報超時異常
                // .block(Duration.ofMillis(0))
                // 1次獲取10個數據
                .count(10);

        // 紀錄最後一次 recordId
        StringBuilder readOffset = new StringBuilder("0-0");
        threadPoolExecutor.execute(() -> {
            while (!stop) {
                // 此時Stream可以理解成普通的list，但是Stream中的消息在讀取後不會消失, 如果需要刪除需再透過 修剪stream 方法移除stream 內容
                // 使用XREAD讀取數據時，需要記錄下最後一次讀取到offset，然後當作下次讀取的offset，否則讀取出來的數據會有問題
                List<ObjectRecord<String, Book>> objectRecords = redisTemplate.opsForStream()
                        .read(Book.class, streamReadOptions, StreamOffset.create(Constants.STREAM_KEY_001, ReadOffset.from(readOffset.toString())));
                if (CollectionUtils.isEmpty(objectRecords)) {
                    log.warn("沒有獲取到數據");
                    continue;
                }
                for (ObjectRecord<String, Book> objectRecord : objectRecords) {
                    log.info("NonBlockConsumer01 >> 獲取到的數據信息 id:{}", objectRecord.getId());
                    readOffset.setLength(0);
                    readOffset.append(objectRecord.getId());
                }
            }
        });
    }

    @Override
    public void destroy() throws Exception {
        stop = true;
        threadPoolExecutor.shutdown();
        threadPoolExecutor.awaitTermination(3, TimeUnit.SECONDS);
    }
}
