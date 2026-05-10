package com.panda.dto;

import lombok.Data;

@Data
public class ParkingPointListDTO {
    private Integer page;
    private Integer pagesize;
    private String keyword;
}
