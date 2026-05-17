package com.panda.domain.ride.unlock.loader;

import com.panda.domain.ride.unlock.model.UnlockContext;
import com.panda.domain.ride.unlock.model.UnlockType;
import com.panda.entity.Scooter;
import com.panda.mapper.DispatchRecordMapper;
import com.panda.mapper.RentalOrderMapper;
import com.panda.mqtt.ScooterOnlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserUnlockContextLoader implements UnlockContextLoader {

    private final RentalOrderMapper rentalOrderMapper;
    private final DispatchRecordMapper dispatchRecordMapper;
    private final ScooterOnlineService scooterOnlineService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public UnlockType supportType() {
        return UnlockType.USER;
    }

    @Override
    public UnlockContext load(Long userId, Scooter scooter, String reserveToken) {
        return UnlockContext.builder()
                .type(UnlockType.USER)
                .userId(userId)
                .scooter(scooter)
                .reserveToken(reserveToken)
                .unpaidOrder(rentalOrderMapper.getUnpaidOrderByUserId(userId))
                .ridingOrder(rentalOrderMapper.getRidingOrderByUserId(userId))
                .activeDispatchRecord(dispatchRecordMapper.getActiveRecordByScooterId(scooter.getId()))
                .scooterOnline(scooterOnlineService.isOnline(scooter.getCode()))
                .userReserved(stringRedisTemplate.hasKey(userReserveKey(userId)))
                .scooterReserved(stringRedisTemplate.hasKey(scooterReserveKey(scooter.getId())))
                .build();
    }

    private String userReserveKey(Long userId) {
        return "panda:ride:user:" + userId + ":reserve";
    }

    private String scooterReserveKey(Long scooterId) {
        return "panda:ride:scooter:" + scooterId + ":reserve";
    }
}
