package com.panda.domain.ride.unlock.loader;

import com.panda.domain.ride.unlock.model.UnlockContext;
import com.panda.domain.ride.unlock.model.UnlockType;
import com.panda.entity.Scooter;

public interface UnlockContextLoader {

    UnlockType supportType();

    UnlockContext load(Long operatorId, Scooter scooter, String reserveToken);
}
