package com.caster.redis.controller;


import com.caster.redis.constan.Constants;
import com.caster.redis.stream.producer.StreamProducer;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 發送消息到Stream
 * 
 * Author: Minchang Hsu (Caster)
 * Date: 2022/12/30
 */
@RestController
@AllArgsConstructor
public class ProducerController {

    private final StreamProducer streamProducer;

    @GetMapping("sendBookToStream")
    public ResponseEntity sendBookToStream() {
        streamProducer.sendRecord(Constants.STREAM_KEY_001);
        return ResponseEntity.ok(200);
    }
}
