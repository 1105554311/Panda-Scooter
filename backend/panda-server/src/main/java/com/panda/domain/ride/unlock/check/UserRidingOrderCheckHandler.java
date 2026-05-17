package com.panda.domain.ride.unlock.check;

import com.panda.domain.ride.unlock.model.UnlockContext;
import com.panda.domain.ride.unlock.model.UnlockType;
import com.panda.exception.BaseException;
import org.springframework.stereotype.Component;

@Component
public class UserRidingOrderCheckHandler implements UnlockCheckHandler {

    @Override
    public int order() {
        return 20;
    }

    @Override
    public boolean supports(UnlockType type) {
        return UnlockType.USER == type;
    }

    @Override
    public void check(UnlockContext context) {
        if (context.getRidingOrder() != null) {
            throw new BaseException("用户正在骑行中");
        }
    }
}
