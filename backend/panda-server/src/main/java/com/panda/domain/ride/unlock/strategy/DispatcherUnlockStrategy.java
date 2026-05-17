package com.panda.domain.ride.unlock.strategy;

import com.panda.domain.ride.unlock.check.UnlockCheckHandler;
import com.panda.domain.ride.unlock.model.UnlockType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DispatcherUnlockStrategy extends AbstractUnlockStrategy {

    public DispatcherUnlockStrategy(List<UnlockCheckHandler> handlers) {
        super(handlers);
    }

    @Override
    public UnlockType supportType() {
        return UnlockType.DISPATCHER;
    }
}
