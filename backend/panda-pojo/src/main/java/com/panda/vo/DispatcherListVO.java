package com.panda.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DispatcherListVO {
    private List<DispatcherItem> dispatcherList;
    private Integer page;
    private Integer pageSize;
    private Integer total;

    @Data
    @Builder
    public static class DispatcherItem {
        private Long id;
        private String name;
        private String email;
        private Long areaId;
        private LocalDateTime createTime;
    }
}