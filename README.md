# 目的

學習與測試 Redis stream 應用技巧及場景
重新翻寫來原作者的專案，當然原作者可能寫得比較好😂。


參考來源：
1. https://segmentfault.com/a/1190000040946712
# 提前創建消費組

`mkstream` 表示如果這個Stream不存在，則會自動創建出來。

```shell
redis-cli -h 127.0.0.1 -p 6379 -a cPKGpSGvky
```
```
del stream-001
```
```
XGROUP CREATE stream-001 group-a $ mkstream
```
```
XGROUP CREATE stream-001 group-b $ mkstream
```
```
xinfo stream stream-001
```

# Class 說明

```
redis
├── CycleGeneratorStreamMessageRunner.java # Stream消息生產者，每隔5s產生一個消息
├── RedisStreamApplication.java
├── config
│   └── RedisConfig.java # redis 配置
├── constan
│   └── Cosntants.java # 常數
├── entity
│   └── Book.java # 實體類
└── stream
    ├── consumer # 消費者
    │   │── AsyncConsumeStreamListener.java # 異步消費消息
    │   │── CustomErrorHandler.java # 處理消費消息或讀取消息過程中發生的異常
    │   │── RedisStreamConfiguration.java # Stream 消費組消費消息
    │   └── impl # 實作消費者，用XREAD讀取stream數據，可以獲取到Stream中所有的消息, 
    │       ├── NonBlockConsumer01.java    # 非阻塞消費者
    │       └── NonBlockConsumer02.java    # 非阻塞消費者 消費者02和消費者01實現的功能一樣，可以看到同一個消息2個消費者都可以消費到
    └── producer
        └── StreamProducer.java # 向Stream中發送消息

```

# RedisTemplate HashValue序列化器選擇錯誤導致的異常

```
java.lang.IllegalArgumentException: Value must not be null!
	at org.springframework.util.Assert.notNull(Assert.java:201)
	at org.springframework.data.redis.connection.stream.Record.of(Record.java:81)
	at org.springframework.data.redis.connection.stream.MapRecord.toObjectRecord(MapRecord.java:147)
	at org.springframework.data.redis.core.StreamObjectMapper.toObjectRecord(StreamObjectMapper.java:138)
	at org.springframework.data.redis.core.StreamObjectMapper.toObjectRecords(StreamObjectMapper.java:164)
	at org.springframework.data.redis.core.StreamOperations.map(StreamOperations.java:594)
	at org.springframework.data.redis.core.StreamOperations.read(StreamOperations.java:413)
	at com.huan.study.redis.stream.consumer.xread.XreadNonBlockConsumer02.lambda$afterPropertiesSet$1(XreadNonBlockConsumer02.java:61)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)
```

如果出現了上述異常，
1. 那麼我們需要檢查一下 `RedisTemplate`的配置，此處可以考慮使用 `redisTemplate.setHashValueSerializer(RedisSerializer.string())`
2. 檢查 `redisTemplate.opsForStream()` 的配置，這個構造方法中是不是填寫了別的HashMapper實現

提供一個可用的配置
1. RedisTemplate 的 setHashValueSerializer(RedisSerializer.string())
2. redisTemplate.opsForStream() 構造方法不用填寫別的HashMapper的實現，就使用默認的ObjectHashMapper

關於上面的這個錯誤，我在Spring Data Redis的官方倉庫提了一個 issue，得到官方的回覆是，這是一個bug，後期會修覆的。
[官方回答](https://github.com/spring-projects/spring-data-redis/issues/2198)

> 2.7 M3 這個版本官方已經修覆了這個bug。 詳情請求看這個issues https://github.com/spring-projects/spring-data-redis/issues/2198


# 注意事項

1. stream 中的 recordId 必須是單調遞增的，可以讓redis自動生成,也可以自己提供。
2. xread 讀取到消息後，需要將讀取到的最後一個消息的recordId當作下一次讀取的id，否則讀取到的數據會有問題。
3. xread 取消消息阻塞的時間需要小於`spring.redis.timeout`配置的時間，否則會報超時錯誤。
4. `StreamMessageListenerContainer` 可以同時支持消費者組消費和獨立消費。
5. `StreamMessageListenerContainer` 可以動態的增加或刪除消費者。
6. 消費組消費時，如果不是自動ack，則需要手動ack。
7. 如果需要對某個消費者進行個性化配置在調用register方法的時候傳遞`StreamReadRequest`對象
8. 因為持久化，所以後續要整理stream 時，需自行修剪stream。 Redis Documentation: [XTRIM](https://redis.io/commands/xtrim/)