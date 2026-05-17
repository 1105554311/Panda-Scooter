package com.panda.domain.ride.unlock.check;

import com.panda.domain.ride.unlock.model.UnlockContext;
import com.panda.domain.ride.unlock.model.UnlockType;
import com.panda.exception.BaseException;
import org.springframework.stereotype.Component;

@Component
public class ScooterAvailableCheckHandler implements UnlockCheckHandler {

    @Override
    public int order() {
        return 30;
    }

    @Override
    public boolean supports(UnlockType type) {
        return UnlockType.USER == type || UnlockType.DISPATCHER == type;
    }

    @Override
    public void check(UnlockContext context) {
        if (!Integer.valueOf(0).equals(context.getScooter().getRideStatus())) {
            throw new BaseException("车辆当前不可用");
        }
    }
}
