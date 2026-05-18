package com.panda.domain.ride.order;

import com.panda.domain.ride.concurrency.RideConcurrencyService;
import com.panda.entity.RentalOrder;
import com.panda.mapper.RentalOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "${panda.ride.unpaid-order-close-topic}",
        consumerGroup = "${panda.ride.unpaid-order-close-consumer-group}"
)
public class UnpaidOrderCloseConsumer implements RocketMQListener<String> {

    private final RentalOrderMapper rentalOrderMapper;
    private final RideConcurrencyService rideConcurrencyService;

    @Override
    public void onMessage(String message) {
        Long orderId = parseOrderId(message);
        if (orderId == null) {
            log.warn("跳过非法未支付订单关闭消息，message={}", message);
            return;
        }
        closeIfStillUnpaid(orderId);
    }

    private Long parseOrderId(String message) {
        try {
            return Long.valueOf(message);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void closeIfStillUnpaid(Long orderId) {
        rideConcurrencyService.withOrderLock(orderId, () -> {
            RentalOrder rentalOrder = rentalOrderMapper.getById(orderId);
            if (rentalOrder == null) {
                log.warn("跳过未支付订单超时关闭，订单不存在，orderId={}", orderId);
                return null;
            }
            if (!Integer.valueOf(1).equals(rentalOrder.getOrderStatus())
                    || !Integer.valueOf(0).equals(rentalOrder.getPayStatus())) {
                log.info("跳过未支付订单超时关闭，订单状态已变化，orderId={}, orderStatus={}, payStatus={}",
                        orderId, rentalOrder.getOrderStatus(), rentalOrder.getPayStatus());
                return null;
            }

            int affectedRows = rentalOrderMapper.closeUnpaidOrder(orderId, LocalDateTime.now());
            if (affectedRows > 0) {
                rideConcurrencyService.clearUnpaidOrder(rentalOrder.getUserId());
                log.info("未支付订单已超时关闭，orderId={}, userId={}", orderId, rentalOrder.getUserId());
            }
            return null;
        });
    }
}
