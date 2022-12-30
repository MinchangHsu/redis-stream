package com.caster.redis.stream.producer;

import com.caster.redis.entity.Book;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 消息生產者
 *
 * Author: Minchang Hsu (Caster)
 * Date: 2022/12/30
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StreamProducer {

    private final RedisTemplate<String, Object> redisTemplate;

    public void sendRecord(String streamKey) {
        Book book = Book.create();
        ObjectRecord<String, Book> record = StreamRecords.newRecord()
                .in(streamKey)
                .ofObject(book)
                .withId(RecordId.autoGenerate());

        RecordId recordId = redisTemplate.opsForStream().add(record);
        log.info("產生一本書的資訊:{}, add stream return recordId:{}", book, recordId);
    }
}
