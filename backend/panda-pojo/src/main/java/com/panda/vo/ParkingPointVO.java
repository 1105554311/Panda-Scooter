package com.panda.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ParkingPointVO {
    private Long id;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer status;
    private LocalDateTime createTime;
}
