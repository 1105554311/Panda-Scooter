package com.panda.domain.ride.unlock.check;

import com.panda.domain.ride.unlock.model.UnlockContext;
import com.panda.domain.ride.unlock.model.UnlockType;

public interface UnlockCheckHandler {

    int order();

    boolean supports(UnlockType type);

    void check(UnlockContext context);
}
