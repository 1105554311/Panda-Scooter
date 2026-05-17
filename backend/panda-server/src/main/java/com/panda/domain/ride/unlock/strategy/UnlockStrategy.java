package com.panda.domain.ride.unlock.strategy;

import com.panda.domain.ride.unlock.model.UnlockContext;
import com.panda.domain.ride.unlock.model.UnlockType;

public interface UnlockStrategy {

    UnlockType supportType();

    void check(UnlockContext context);
}
