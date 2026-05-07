package com.panda.dto;

import lombok.Data;

@Data
public class ZoneListDTO {
    private Integer page;
    private Integer pageSize;
    private String keyword;
    private Long adminId;
}