package com.panda.domain.ride.unlock.loader;

import com.panda.domain.ride.unlock.model.UnlockContext;
import com.panda.domain.ride.unlock.model.UnlockType;
import com.panda.entity.Scooter;
import com.panda.mapper.DispatchRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DispatcherUnlockContextLoader implements UnlockContextLoader {

    private final DispatchRecordMapper dispatchRecordMapper;

    @Override
    public UnlockType supportType() {
        return UnlockType.DISPATCHER;
    }

    @Override
    public UnlockContext load(Long dispatcherId, Scooter scooter, String reserveToken) {
        return UnlockContext.builder()
                .type(UnlockType.DISPATCHER)
                .dispatcherId(dispatcherId)
                .scooter(scooter)
                .activeDispatchRecord(dispatchRecordMapper.getActiveRecordByScooterId(scooter.getId()))
                .build();
    }
}
