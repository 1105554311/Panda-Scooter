package com.panda.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminScooterListVO {
    private List<ScooterItem> areaList;
    private Integer areaId;
    private String scooterCount;

    @Data
    @Builder
    public static class ScooterItem {
        private Long id;
        private String code;
        private Integer rideStatus;
        private Integer faultStatus;
        private Integer battery;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private LocalDateTime createTime;
    }
}
