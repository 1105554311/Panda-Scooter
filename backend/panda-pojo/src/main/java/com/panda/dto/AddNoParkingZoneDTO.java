package com.panda.dto;

import lombok.Data;

@Data
public class AddNoParkingZoneDTO {
    private String name;
    private String polygon;
    private Integer status;
}
