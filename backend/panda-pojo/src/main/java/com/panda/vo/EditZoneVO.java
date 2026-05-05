package com.panda.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EditZoneVO {
    private ZoneInfo zone;
    private DispatcherInfo dispatcher;

    @Data
    @Builder
    public static class ZoneInfo {
        private Long id;
        private String name;
        private String polygon;
        private String createTime;
    }

    @Data
    @Builder
    public static class DispatcherInfo {
        private Long id;
        private String name;
        private String email;
        private Long areaId;
    }
}