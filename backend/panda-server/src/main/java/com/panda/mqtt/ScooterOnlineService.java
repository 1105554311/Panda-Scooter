package com.panda.mqtt;

import com.panda.properties.MqttProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ScooterOnlineService {

    private static final String ONLINE_KEY_PREFIX = "panda:scooter:online:";

    private final StringRedisTemplate stringRedisTemplate;
    private final MqttProperties mqttProperties;

    public void refreshOnline(String scooterCode) {
        if (scooterCode == null || scooterCode.isBlank()) {
            return;
        }
        stringRedisTemplate.opsForValue().set(
                buildOnlineKey(scooterCode),
                String.valueOf(System.currentTimeMillis()),
                Duration.ofSeconds(mqttProperties.getScooterOnlineTtlSeconds())
        );
    }

    public boolean isOnline(String scooterCode) {
        if (scooterCode == null || scooterCode.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(buildOnlineKey(scooterCode)));
    }

    private String buildOnlineKey(String scooterCode) {
        return ONLINE_KEY_PREFIX + scooterCode;
    }
}
