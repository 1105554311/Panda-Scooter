package com.panda.dto;

import lombok.Data;

@Data
public class DeleteDispatcherDTO {
    private Long dispatcherId;
    private String name;
    private String email;
}
