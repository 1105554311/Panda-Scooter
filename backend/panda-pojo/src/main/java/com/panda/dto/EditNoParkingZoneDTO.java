package com.panda.dto;

import lombok.Data;

@Data
public class EditNoParkingZoneDTO {
    private Long id;
    private String name;
    private String polygon;
    private Integer status;
    private String createTime;
}
