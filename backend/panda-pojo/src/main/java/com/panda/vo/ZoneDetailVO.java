package com.panda.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ZoneDetailVO {
    private Long id;
    private String name;
    private String polygon;
    private LocalDateTime createTime;
    private List<DispatcherInfo> dispatchers;
    private Integer vehicleCount;

    @Data
    @Builder
    public static class DispatcherInfo {
        private Long id;
        private String name;
        private String email;
        private Long areaId;
    }
}
