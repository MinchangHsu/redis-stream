package com.caster.redis.stream.consumer;


import com.caster.redis.constan.Constants;
import com.caster.redis.entity.Book;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.hash.ObjectHashMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * redis stream 消費組配置
 *
 * Author: Minchang Hsu (Caster)
 * Date: 2022/12/30
 */
@Configuration
public class RedisStreamConfiguration {

    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * 可以同時支援 獨立消費 和 消費者組 消費
     * <p>
     * 可以支援動態的 增加和刪除 消費者
     * <p>
     * <p>
     * 支援消費者發生異常後，還可以繼續消費，消費者不會被剔除，通過StreamReadRequest的cancelOnError來實現
     * </p>
     * 消費組需要預先創建出來
     *
     * @return StreamMessageListenerContainer
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public StreamMessageListenerContainer<String, ObjectRecord<String, Book>> streamMessageListenerContainer() {
        AtomicInteger index = new AtomicInteger(1);
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(processors, processors, 0, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(), r -> {
            Thread thread = new Thread(r);
            thread.setName("async-stream-consumer-" + index.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, Book>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        // 一次最多獲取多少條消息
                        .batchSize(10)
                        // 運行 Stream 的 poll task
                        .executor(executor)
                        // 可以理解為 Stream Key 的序列化方式
                        .keySerializer(RedisSerializer.string())
                        // 可以理解為 Stream 後方的字段的 key 的序列化方式
                        .hashKeySerializer(RedisSerializer.string())
                        // 可以理解為 Stream 後方的字段的 value 的序列化方式
                        .hashValueSerializer(RedisSerializer.string())
                        // Stream 中沒有消息時，阻塞多長時間，需要比 `spring.redis.timeout` 的時間小
                        .pollTimeout(Duration.ofSeconds(1))
                        // ObjectRecord 時，將 對象的 filed 和 value 轉換成一個 Map 比如：將Book對象轉換成map
                        .objectMapper(new ObjectHashMapper())
                        // 獲取消息的過程或獲取到消息給具體的消息者處理的過程中，發生了異常的處理
                        .errorHandler(new CustomErrorHandler())
                        // 將發送到Stream中的Record轉換成ObjectRecord，轉換成具體的類型是這個地方指定的類型
                        .targetType(Book.class)
                        .build();

        StreamMessageListenerContainer<String, ObjectRecord<String, Book>> streamMessageListenerContainer =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        // 獨立消費
        {
            // 目前還不清楚此獨立消費, 使用場景是什麼.
            streamMessageListenerContainer.receive(StreamOffset.fromStart(Constants.STREAM_KEY_001),
                    new AsyncConsumeStreamListener(Constants.singleConsume, null, null));
        }

        // 消費群組
        {
            // 群組A
            {
                // 不自動ack
                streamMessageListenerContainer.receive(Consumer.from(Constants.group_a, "consumer-a"),
                        StreamOffset.create(Constants.STREAM_KEY_001, ReadOffset.lastConsumed()),
                        new AsyncConsumeStreamListener(Constants.groupConsume, Constants.group_a, "consumer-a"));

                // 自動 ack
                streamMessageListenerContainer.receiveAutoAck(Consumer.from(Constants.group_a, "consumer-a"),
                        StreamOffset.create(Constants.STREAM_KEY_001, ReadOffset.lastConsumed()),
                        new AsyncConsumeStreamListener(Constants.groupConsume, Constants.group_a, "consumer-a"));

                streamMessageListenerContainer.receive(Consumer.from(Constants.group_a, "consumer-b"),
                        StreamOffset.create(Constants.STREAM_KEY_001, ReadOffset.lastConsumed()),
                        new AsyncConsumeStreamListener(Constants.groupConsume, Constants.group_a, "consumer-b"));


                // 可自定義 consumer 消費動作 > 如果需要對某個消費者進行自定義配置時, 需使用register方法的時, 傳入`StreamReadRequest`對象.
                // consumer-c，在消費發生異常時，還可以繼續消費
                StreamMessageListenerContainer.ConsumerStreamReadRequest<String> streamReadRequest = StreamMessageListenerContainer
                        .StreamReadRequest
                        .builder(StreamOffset.create(Constants.STREAM_KEY_001, ReadOffset.lastConsumed()))
                        .consumer(Consumer.from(Constants.group_a, "consumer-c"))
                        .autoAcknowledge(true)
                        // 如果消費者發生了異常，判斷是否取消消費者消費
                        .cancelOnError(throwable -> false)
                        .build();
                streamMessageListenerContainer.register(streamReadRequest,
                        new AsyncConsumeStreamListener(Constants.groupConsume, Constants.group_a, "consumer-c"));
            }

            // 群組B
            {
                // 自動ack
                streamMessageListenerContainer.receiveAutoAck(Consumer.from(Constants.group_b, "consumer-1"),
                        StreamOffset.create(Constants.STREAM_KEY_001, ReadOffset.lastConsumed()),
                        new AsyncConsumeStreamListener(Constants.groupConsume, Constants.group_b, "consumer-1"));
            }
        }
        return streamMessageListenerContainer;
    }
}
