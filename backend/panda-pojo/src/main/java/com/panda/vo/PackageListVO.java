package com.panda.vo;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PackageListVO {
    private Integer total;
    private Integer page;
    private Integer pageSize;
    private List<PackageItem> list;

    @Data
    @Builder
    public static class PackageItem {
        private Long id;
        private String title;
        private String description;
        private Integer type;
        private BigDecimal price;
        private String createTime;
    }
}