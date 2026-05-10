package com.panda.dto;

import lombok.Data;

@Data
public class DispatcherListDTO {
    private Integer page;
    private Integer pagesize;
    private String keyword;
    private Long areaId;
}
