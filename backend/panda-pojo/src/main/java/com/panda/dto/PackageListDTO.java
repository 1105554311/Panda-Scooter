package com.panda.dto;

import lombok.Data;

@Data
public class PackageListDTO {
    private Integer page;
    private Integer pageSize;
    private String status;
    private String keyword;
}
