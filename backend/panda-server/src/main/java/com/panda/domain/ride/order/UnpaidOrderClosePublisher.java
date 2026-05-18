package com.panda.domain.ride.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnpaidOrderClosePublisher {

    private static final int CLOSE_DELAY_SECONDS = 1 * 60;

    private final RocketMQTemplate rocketMQTemplate;

    @Value("${panda.ride.unpaid-order-close-topic}")
    private String closeTopic;

    public void publishDelayCloseMessage(Long orderId) {
        if (orderId == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendDelayMessage(orderId);
                }
            });
            return;
        }
        sendDelayMessage(orderId);
    }

    private void sendDelayMessage(Long orderId) {
        rocketMQTemplate.syncSendDelayTimeSeconds(closeTopic, String.valueOf(orderId), CLOSE_DELAY_SECONDS);
        log.info("未支付订单已发送 RocketMQ 延迟关闭消息，orderId={}, delaySeconds={}", orderId, CLOSE_DELAY_SECONDS);
    }
}
