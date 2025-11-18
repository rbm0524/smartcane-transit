package com.smartcane.transit.config;

import com.smartcane.transit.service.TripState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port
    ) {
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    public RedisTemplate<String, TripState> tripRedisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, TripState> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);

        // key는 문자열
        template.setKeySerializer(new StringRedisSerializer());

        // value는 JSON (TripState 직렬화)
        Jackson2JsonRedisSerializer<TripState> valueSerializer =
                new Jackson2JsonRedisSerializer<>(TripState.class);
        template.setValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
