package com.panda.dto;

import lombok.Data;

@Data
public class AddZoneDTO {
    private String name;
    private String polygon;
    private Integer dispatcherId;
}