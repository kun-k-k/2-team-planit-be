package com.planit.domain.trip.service.redisAccessor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planit.domain.trip.config.RedisStreamProperties;
import com.planit.domain.trip.dto.AiItineraryRequest;
import com.planit.domain.trip.dto.AiItineraryResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItineraryJobStreamService {
    private static final Logger log = LoggerFactory.getLogger(ItineraryJobStreamService.class);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisStreamProperties streamProperties;

    public RecordId enqueueJob(Long tripId, AiItineraryRequest request) {
        Map<String, String> fields = new HashMap<>();
        fields.put("tripId", String.valueOf(tripId));
        fields.put("payload", toJson(request));
        fields.put("createdAt", Instant.now().toString());
        StreamOperations<String, String, String> ops = redisTemplate.opsForStream();
        RecordId id = ops.add(MapRecord.create(streamProperties.getAiJobsKey(), fields));
        log.info("[STREAM] jobs XADD tripId={}, recordId={}", tripId, id == null ? "null" : id.getValue());
        return id;
    }

    //AI모킹용 결과큐 발행 메서드
    public RecordId publishResult(Long tripId, String status, AiItineraryResponse response, String errorMessage) {
        Map<String, String> fields = new HashMap<>();
        fields.put("tripId", String.valueOf(tripId));
        fields.put("status", status);
        fields.put("payload", response != null ? toJson(response) : "");
        fields.put("errorMessage", errorMessage == null ? "" : errorMessage);
        fields.put("finishedAt", Instant.now().toString());
        StreamOperations<String, String, String> ops = redisTemplate.opsForStream();
        RecordId id = ops.add(MapRecord.create(streamProperties.getAiResultsKey(), fields));
        log.info("[STREAM] results XADD tripId={}, status={}, recordId={}", tripId, status, id == null ? "null" : id.getValue());
        return id;
    }

    // AI모킹용 잡상태 갱신 메서드 (추가하기)



    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }
}
