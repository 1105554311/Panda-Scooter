package com.panda.mqtt;

import com.panda.entity.ScooterCommand;
import com.panda.mapper.ScooterCommandMapper;
import com.panda.properties.MqttProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScooterCommandRetryTask {

    private final MqttProperties mqttProperties;
    private final ScooterCommandMapper scooterCommandMapper;
    private final ScooterMqttPublisher scooterMqttPublisher;

    @Scheduled(fixedDelayString = "#{@mqttProperties.commandRetryScanDelaySeconds * 1000}")
    public void retryTimeoutCommands() {
        if (!Boolean.TRUE.equals(mqttProperties.getEnabled())) {
            return;
        }

        LocalDateTime deadline = LocalDateTime.now().minusSeconds(mqttProperties.getCommandAckTimeoutSeconds());
        int timeoutRows = scooterCommandMapper.markTimeoutCommands(deadline, "MQTT command ack timeout");
        if (timeoutRows > 0) {
            log.warn("Marked scooter commands timeout, rows={}", timeoutRows);
        }

        List<ScooterCommand> commands = scooterCommandMapper.listRetryableTimeoutCommands(
                deadline,
                mqttProperties.getCommandRetryBatchSize()
        );
        for (ScooterCommand command : commands) {
            scooterMqttPublisher.retryCommand(command);
        }
    }
}
