package com.panda.dto;

import lombok.Data;

@Data
public class EditDispatcherDTO {
    private Long id;
    private String name;
    private String password;
    private String email;
    private Long areaId;
    private boolean areaIdPresent;

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
        this.areaIdPresent = true;
    }
}
