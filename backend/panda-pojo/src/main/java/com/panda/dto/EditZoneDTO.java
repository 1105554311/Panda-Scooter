package com.panda.dto;

import lombok.Data;

@Data
public class EditZoneDTO {
    private Long id;
    private String name;
    private String polygon;
    private String createTime;
}