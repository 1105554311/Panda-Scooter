package com.panda.domain.ride.unlock.model;

import com.panda.entity.DispatchRecord;
import com.panda.entity.RentalOrder;
import com.panda.entity.Scooter;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnlockContext {

    private final UnlockType type;
    private final Long userId;
    private final Long dispatcherId;
    private final Scooter scooter;
    private final String reserveToken;
    private final RentalOrder unpaidOrder;
    private final RentalOrder ridingOrder;
    private final DispatchRecord activeDispatchRecord;
    private final boolean scooterOnline;
    private final boolean userReserved;
    private final boolean scooterReserved;
}
