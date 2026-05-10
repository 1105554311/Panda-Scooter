package com.panda.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NoParkingZoneVO {
    private Long id;
    private String name;
    private String polygon;
    private Integer status;
    private LocalDateTime createTime;
    private Integer polygonPointCount;
    private String createdBy;
}
