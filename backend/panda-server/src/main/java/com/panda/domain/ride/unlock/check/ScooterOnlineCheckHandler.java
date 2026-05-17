package com.panda.domain.ride.unlock.check;

import com.panda.domain.ride.unlock.model.UnlockContext;
import com.panda.domain.ride.unlock.model.UnlockType;
import com.panda.exception.BaseException;
import org.springframework.stereotype.Component;

@Component
public class ScooterOnlineCheckHandler implements UnlockCheckHandler {

    @Override
    public int order() {
        return 60;
    }

    @Override
    public boolean supports(UnlockType type) {
        return UnlockType.USER == type;
    }

    @Override
    public void check(UnlockContext context) {
        if (context.isScooterOnline()) {
            throw new BaseException("车辆离线，暂不可用");
        }
    }
}
