package com.panda.domain.ride.concurrency;

import com.panda.domain.ride.unlock.loader.UnlockContextLoaderRouter;
import com.panda.domain.ride.unlock.model.UnlockContext;
import com.panda.domain.ride.unlock.model.UnlockType;
import com.panda.domain.ride.unlock.strategy.UnlockStrategyRouter;
import com.panda.entity.Scooter;
import com.panda.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RideConcurrencyService {

    private static final Duration STATE_TTL = Duration.ofHours(2);
    private static final long LOCK_WAIT_SECONDS = 3L;
    private static final long LOCK_LEASE_SECONDS = 15L;

    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final UnlockContextLoaderRouter unlockContextLoaderRouter;
    private final UnlockStrategyRouter unlockStrategyRouter;

    public <T> T withUserScooterLocks(Long userId, Long scooterId, Supplier<T> supplier) {
        RLock userLock = redissonClient.getLock("panda:lock:ride:user:" + userId);
        RLock scooterLock = redissonClient.getLock("panda:lock:ride:scooter:" + scooterId);
        return withLocks(List.of(userLock, scooterLock), supplier);
    }

    public <T> T withDispatcherScooterLocks(Long dispatcherId, Long scooterId, Supplier<T> supplier) {
        RLock dispatcherLock = redissonClient.getLock("panda:lock:dispatch:dispatcher:" + dispatcherId);
        RLock scooterLock = redissonClient.getLock("panda:lock:ride:scooter:" + scooterId);
        return withLocks(List.of(dispatcherLock, scooterLock), supplier);
    }

    public <T> T withOrderLock(Long orderId, Supplier<T> supplier) {
        return withLocks(List.of(redissonClient.getLock("panda:lock:ride:order:" + orderId)), supplier);
    }

    public <T> T withUserOrderLocks(Long userId, Long orderId, Supplier<T> supplier) {
        RLock userLock = redissonClient.getLock("panda:lock:ride:user:" + userId);
        RLock orderLock = redissonClient.getLock("panda:lock:ride:order:" + orderId);
        return withLocks(List.of(userLock, orderLock), supplier);
    }

    public String prepareUnlockAndReserve(Long userId, Scooter scooter) {
        String reserveToken = UUID.randomUUID().toString();
        UnlockContext context = unlockContextLoaderRouter.load(UnlockType.USER, userId, scooter, reserveToken);
        unlockStrategyRouter.check(context);
        return reserveToken;
    }

    public void prepareDispatcherUnlock(Long dispatcherId, Scooter scooter) {
        UnlockContext context = unlockContextLoaderRouter.load(UnlockType.DISPATCHER, dispatcherId, scooter, null);
        unlockStrategyRouter.check(context);
    }

    public void markUnlockSuccess(Long userId, Scooter scooter, Long orderId) {
        stringRedisTemplate.opsForValue().set(userRidingKey(userId), String.valueOf(orderId), STATE_TTL);
        stringRedisTemplate.opsForHash().put(scooterStateKey(scooter.getId()), "rideStatus", "1");
        stringRedisTemplate.expire(scooterStateKey(scooter.getId()), STATE_TTL);
        releaseUnlockReservation(userId, scooter.getId());
    }

    public void markLockSuccess(Long userId, Long scooterId) {
        stringRedisTemplate.delete(userRidingKey(userId));
        stringRedisTemplate.opsForHash().put(scooterStateKey(scooterId), "rideStatus", "0");
        stringRedisTemplate.expire(scooterStateKey(scooterId), STATE_TTL);
    }

    public void markUnpaidOrder(Long userId, Long orderId) {
        stringRedisTemplate.delete(userRidingKey(userId));
        stringRedisTemplate.opsForValue().set(userUnpaidKey(userId), String.valueOf(orderId), STATE_TTL);
    }

    public void clearUnpaidOrder(Long userId) {
        stringRedisTemplate.delete(userUnpaidKey(userId));
    }

    public void releaseUnlockReservation(Long userId, Long scooterId) {
        stringRedisTemplate.delete(List.of(userReserveKey(userId), scooterReserveKey(scooterId)));
    }

    private <T> T withLocks(List<RLock> locks, Supplier<T> supplier) {
        List<RLock> acquiredLocks = new java.util.ArrayList<>();
        try {
            for (RLock lock : locks) {
                if (!lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS)) {
                    throw new BaseException("操作太频繁，请稍后重试");
                }
                acquiredLocks.add(lock);
            }
            return supplier.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BaseException("操作被中断，请稍后重试");
        } finally {
            for (int i = acquiredLocks.size() - 1; i >= 0; i--) {
                RLock lock = acquiredLocks.get(i);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    private String userUnpaidKey(Long userId) {
        return "panda:ride:user:" + userId + ":unpaid";
    }

    private String userRidingKey(Long userId) {
        return "panda:ride:user:" + userId + ":riding";
    }

    private String userReserveKey(Long userId) {
        return "panda:ride:user:" + userId + ":reserve";
    }

    private String scooterStateKey(Long scooterId) {
        return "panda:ride:scooter:" + scooterId + ":state";
    }

    private String scooterReserveKey(Long scooterId) {
        return "panda:ride:scooter:" + scooterId + ":reserve";
    }
}
