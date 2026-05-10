package com.panda.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ParkingPointListVO {
    private List<ParkingPointItem> areaList;
    private Integer page;
    private Integer pagesize;
    private Integer total;

    @Data
    @Builder
    public static class ParkingPointItem {
        private Long id;
        private String name;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private Integer status;
        private LocalDateTime createTime;
    }
}
