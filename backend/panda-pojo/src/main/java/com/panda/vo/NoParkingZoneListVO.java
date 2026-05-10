package com.panda.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class NoParkingZoneListVO {
    private List<NoParkingZoneItem> areaList;
    private Integer page;
    private Integer pagesize;
    private Integer total;
    private Integer vehicleCount;

    @Data
    @Builder
    public static class NoParkingZoneItem {
        private Long id;
        private String name;
        private String polygon;
        private Integer status;
        private LocalDateTime createTime;
    }
}
