package com.panda.dto;

import lombok.Data;

@Data
public class PackageListDTO {
    private Integer page;
    private Integer pagesize;
    private String keyword;
}
