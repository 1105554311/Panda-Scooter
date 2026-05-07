package com.panda.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class EditDispatcherVO {
    private DispatcherInfo dispatcher;

    @Data
    @Builder
    public static class DispatcherInfo {
        private Long id;
        private String name;
        private String email;
        private Long areaId;
        private Integer status;
        private LocalDateTime createTime;
    }
}