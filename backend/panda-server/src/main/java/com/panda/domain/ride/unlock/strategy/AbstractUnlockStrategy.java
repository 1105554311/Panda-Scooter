package com.panda.domain.ride.unlock.strategy;

import com.panda.domain.ride.unlock.check.UnlockCheckHandler;
import com.panda.domain.ride.unlock.model.UnlockContext;

import java.util.Comparator;
import java.util.List;

public abstract class AbstractUnlockStrategy implements UnlockStrategy {

    private final List<UnlockCheckHandler> handlers;

    protected AbstractUnlockStrategy(List<UnlockCheckHandler> handlers) {
        this.handlers = handlers.stream()
                .filter(handler -> handler.supports(supportType()))
                .sorted(Comparator.comparingInt(UnlockCheckHandler::order))
                .toList();
    }

    @Override
    public void check(UnlockContext context) {
        for (UnlockCheckHandler handler : handlers) {
            handler.check(context);
        }
    }
}
