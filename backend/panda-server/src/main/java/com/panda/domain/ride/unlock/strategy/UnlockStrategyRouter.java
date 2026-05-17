package com.panda.domain.ride.unlock.strategy;

import com.panda.domain.ride.unlock.model.UnlockContext;
import com.panda.domain.ride.unlock.model.UnlockType;
import com.panda.exception.BaseException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class UnlockStrategyRouter {

    private final Map<UnlockType, UnlockStrategy> strategyMap = new EnumMap<>(UnlockType.class);

    public UnlockStrategyRouter(List<UnlockStrategy> strategies) {
        for (UnlockStrategy strategy : strategies) {
            strategyMap.put(strategy.supportType(), strategy);
        }
    }

    public void check(UnlockContext context) {
        UnlockStrategy strategy = strategyMap.get(context.getType());
        if (strategy == null) {
            throw new BaseException("不支持的开锁类型");
        }
        strategy.check(context);
    }
}
