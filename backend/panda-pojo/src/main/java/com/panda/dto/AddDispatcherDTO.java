package com.panda.dto;

import lombok.Data;

@Data
public class AddDispatcherDTO {
    private String name;
    private String password;
    private String email;
    private Long areaId;
}