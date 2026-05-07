package com.panda.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ZoneListVO {
    private List<ZoneItem> areaList;
    private Integer page;
    private Integer pageSize;
    private Integer total;

    @Data
    @Builder
    public static class ZoneItem {
        private Long id;
        private String name;
        private String polygon;
        private LocalDateTime createTime;
    }
}