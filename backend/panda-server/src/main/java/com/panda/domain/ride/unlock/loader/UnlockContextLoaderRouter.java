package com.panda.domain.ride.unlock.loader;

import com.panda.domain.ride.unlock.model.UnlockContext;
import com.panda.domain.ride.unlock.model.UnlockType;
import com.panda.entity.Scooter;
import com.panda.exception.BaseException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class UnlockContextLoaderRouter {

    private final Map<UnlockType, UnlockContextLoader> loaderMap = new EnumMap<>(UnlockType.class);

    public UnlockContextLoaderRouter(List<UnlockContextLoader> loaders) {
        for (UnlockContextLoader loader : loaders) {
            loaderMap.put(loader.supportType(), loader);
        }
    }

    public UnlockContext load(UnlockType type, Long operatorId, Scooter scooter, String reserveToken) {
        UnlockContextLoader loader = loaderMap.get(type);
        if (loader == null) {
            throw new BaseException("不支持的开锁类型");
        }
        return loader.load(operatorId, scooter, reserveToken);
    }
}
