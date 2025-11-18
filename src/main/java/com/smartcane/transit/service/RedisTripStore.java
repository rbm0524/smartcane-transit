package com.smartcane.transit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Profile("redis") // prod에서만 활성화, local은 in-memory 사용
@RequiredArgsConstructor
public class RedisTripStore implements TripStore {

    private static final String KEY_PREFIX = "trip:";          // trip:UUID 형태로 저장
    private static final Duration TTL = Duration.ofHours(3);   // 3시간 후 자동 만료 (원하는 값으로 조정)

    private final RedisTemplate<String, TripState> tripRedisTemplate;

    private String key(String tripId) {
        return KEY_PREFIX + tripId;
    }

    @Override
    public void init(String tripId, int itineraryIndex, int legIndex, Integer stepIndex, String phase) {
        TripState state = new TripState(tripId, itineraryIndex, legIndex, stepIndex, phase);
        tripRedisTemplate.opsForValue().set(key(tripId), state, TTL);
    }

    @Override
    public TripState load(String tripId) {
        return tripRedisTemplate.opsForValue().get(key(tripId));
    }

    @Override
    public void save(String tripId, TripState state) {
        tripRedisTemplate.opsForValue().set(key(tripId), state, TTL);
    }
}
