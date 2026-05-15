package com.panda.mqtt;

import com.panda.entity.RentalOrder;
import com.panda.entity.Scooter;
import com.panda.entity.ScooterCommand;
import com.panda.mapper.RentalOrderMapper;
import com.panda.mapper.ScooterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScooterCommandBizService {

    private static final String COMMAND_UNLOCK = "UNLOCK";
    private static final String COMMAND_LOCK = "LOCK";
    private static final int ORDER_STATUS_CANCELLED = 3;

    private final RentalOrderMapper rentalOrderMapper;
    private final ScooterMapper scooterMapper;

    @Transactional
    public void handleAckSuccess(ScooterCommand scooterCommand) {
        if (scooterCommand == null) {
            return;
        }
        if (COMMAND_UNLOCK.equals(scooterCommand.getCommandType())) {
            ensureScooterRideStatus(scooterCommand, 1);
        } else if (COMMAND_LOCK.equals(scooterCommand.getCommandType())) {
            ensureScooterRideStatus(scooterCommand, 0);
        }
    }

    @Transactional
    public void handleCommandFailure(ScooterCommand scooterCommand, String reason) {
        if (scooterCommand == null) {
            return;
        }
        if (COMMAND_UNLOCK.equals(scooterCommand.getCommandType())) {
            cancelUnlockOrder(scooterCommand, reason);
        } else if (COMMAND_LOCK.equals(scooterCommand.getCommandType())) {
            log.warn("Lock command failed, commandId={}, orderId={}, scooterCode={}, reason={}",
                    scooterCommand.getCommandId(), scooterCommand.getOrderId(), scooterCommand.getScooterCode(), reason);
        }
    }

    private void cancelUnlockOrder(ScooterCommand scooterCommand, String reason) {
        Long orderId = scooterCommand.getOrderId();
        if (orderId == null) {
            releaseScooter(scooterCommand);
            return;
        }

        RentalOrder rentalOrder = rentalOrderMapper.getById(orderId);
        if (rentalOrder == null) {
            releaseScooter(scooterCommand);
            return;
        }

        int affectedRows = rentalOrderMapper.cancelRidingOrder(orderId, LocalDateTime.now(), ORDER_STATUS_CANCELLED);
        if (affectedRows > 0) {
            scooterMapper.updateRideStatus(rentalOrder.getScooterId(), 0);
            log.warn("Cancelled unlock order because scooter command failed, commandId={}, orderId={}, reason={}",
                    scooterCommand.getCommandId(), orderId, reason);
            return;
        }

        log.warn("Skip cancelling unlock order because order is not riding, commandId={}, orderId={}, orderStatus={}, reason={}",
                scooterCommand.getCommandId(), orderId, rentalOrder.getOrderStatus(), reason);
    }

    private void ensureScooterRideStatus(ScooterCommand scooterCommand, Integer rideStatus) {
        Scooter scooter = resolveScooter(scooterCommand);
        if (scooter == null) {
            return;
        }
        if (!rideStatus.equals(scooter.getRideStatus())) {
            scooterMapper.updateRideStatus(scooter.getId(), rideStatus);
        }
    }

    private void releaseScooter(ScooterCommand scooterCommand) {
        Scooter scooter = resolveScooter(scooterCommand);
        if (scooter != null) {
            scooterMapper.updateRideStatus(scooter.getId(), 0);
        }
    }

    private Scooter resolveScooter(ScooterCommand scooterCommand) {
        if (scooterCommand.getScooterId() != null) {
            return scooterMapper.getById(scooterCommand.getScooterId());
        }
        if (scooterCommand.getScooterCode() != null && !scooterCommand.getScooterCode().isBlank()) {
            return scooterMapper.getByCode(scooterCommand.getScooterCode());
        }
        return null;
    }
}
