package com.panda.dto;

import lombok.Data;

@Data
public class DispatcherListDTO {
    private Integer page;
    private Integer pageSize;
    private String keyword;
    private Long areaId;
}