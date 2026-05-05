package com.panda.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ZoneDetailVO {
    private Long id;
    private String name;
    private String polygon;
    private DispatcherInfo dispatcher;

    @Data
    @Builder
    public static class DispatcherInfo {
        private Long id;
        private String name;
        private String email;
    }
}