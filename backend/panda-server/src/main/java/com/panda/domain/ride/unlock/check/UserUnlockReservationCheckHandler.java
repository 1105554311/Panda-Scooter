package com.panda.domain.ride.unlock.check;

import com.panda.domain.ride.unlock.model.UnlockContext;
import com.panda.domain.ride.unlock.model.UnlockType;
import com.panda.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class UserUnlockReservationCheckHandler implements UnlockCheckHandler {

    private static final Duration RESERVE_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public int order() {
        return 100;
    }

    @Override
    public boolean supports(UnlockType type) {
        return UnlockType.USER == type;
    }

    @Override
    public void check(UnlockContext context) {
        if (context.isUserReserved() || context.isScooterReserved()) {
            throw new BaseException("开锁请求正在处理中，请勿重复提交");
        }
        stringRedisTemplate.opsForValue().set(userReserveKey(context.getUserId()), context.getReserveToken(), RESERVE_TTL);
        stringRedisTemplate.opsForValue().set(scooterReserveKey(context.getScooter().getId()), context.getReserveToken(), RESERVE_TTL);
    }

    private String userReserveKey(Long userId) {
        return "panda:ride:user:" + userId + ":reserve";
    }

    private String scooterReserveKey(Long scooterId) {
        return "panda:ride:scooter:" + scooterId + ":reserve";
    }
}
