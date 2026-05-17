package com.panda.domain.ride.unlock.check;

import com.panda.domain.ride.unlock.model.UnlockContext;
import com.panda.domain.ride.unlock.model.UnlockType;
import com.panda.exception.BaseException;
import org.springframework.stereotype.Component;

@Component
public class ScooterDispatchingCheckHandler implements UnlockCheckHandler {

    @Override
    public int order() {
        return 50;
    }

    @Override
    public boolean supports(UnlockType type) {
        return UnlockType.USER == type || UnlockType.DISPATCHER == type;
    }

    @Override
    public void check(UnlockContext context) {
        if (context.getActiveDispatchRecord() != null) {
            throw new BaseException("车辆正在调度中");
        }
    }
}
