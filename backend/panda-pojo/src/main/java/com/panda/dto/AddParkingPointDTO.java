package com.panda.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddParkingPointDTO {
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer status;
}
