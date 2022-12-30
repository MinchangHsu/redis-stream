package com.caster.redis.stream.consumer;

import com.caster.redis.entity.Book;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;

/**
 * 通過監聽器異步消費
 *
 * Author: Minchang Hsu (Caster)
 * Date: 2022/12/30
 */
@Slf4j
@Getter
@Setter
public class AsyncConsumeStreamListener implements StreamListener<String, ObjectRecord<String, Book>> {
    /**
     * 消費者類型：獨立消費、消費群組
     */
    private String consumerType;
    /**
     * 消費組
     */
    private String group;
    /**
     * 消費組中的某個消費者
     */
    private String consumerName;

    public AsyncConsumeStreamListener(String consumerType, String group, String consumerName) {
        this.consumerType = consumerType;
        this.group = group;
        this.consumerName = consumerName;
    }

    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(ObjectRecord<String, Book> message) {
        String stream = message.getStream();
        RecordId id = message.getId();
        Book value = message.getValue();
        if (StringUtils.isBlank(group)) {
            log.info("[{}]: 接收到一個消息 id:{}", consumerType, id);
        } else {
            log.info("[{}], group:{},  consume:{}, 消息 id:{}", consumerType, group, consumerName, id);
//            redisTemplate.opsForStream().acknowledge(stream, group, id);
        }

        // 當是消費組消費時，如果不是自動ack，則需要在這個地方手動ack, 但獨立消費不知如何手動ack
//         redisTemplate.opsForStream().acknowledge("key","group","recordId");
    }
}
