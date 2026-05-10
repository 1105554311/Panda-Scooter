package com.panda.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EditParkingPointDTO {
    private Long id;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer status;
    private String createTime;
}
