package com.panda.domain.ride.unlock.check;

import com.panda.domain.ride.unlock.model.UnlockContext;
import com.panda.domain.ride.unlock.model.UnlockType;
import com.panda.exception.BaseException;
import org.springframework.stereotype.Component;

@Component
public class ScooterFaultCheckHandler implements UnlockCheckHandler {

    @Override
    public int order() {
        return 40;
    }

    @Override
    public boolean supports(UnlockType type) {
        return UnlockType.USER == type;
    }

    @Override
    public void check(UnlockContext context) {
        if (Integer.valueOf(1).equals(context.getScooter().getFaultStatus())) {
            throw new BaseException("车辆故障，暂不可用");
        }
    }
}
