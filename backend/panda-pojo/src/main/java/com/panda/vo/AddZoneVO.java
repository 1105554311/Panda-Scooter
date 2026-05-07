package com.panda.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddZoneVO {
    private Long id;
    private String name;
    private String polygon;
    private Integer dispatcherId;
    private Integer polygonPointCount;
    private String createdBy;
}