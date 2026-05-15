package com.panda.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScooterCommandMessage {

    private String commandId;

    private String command;

    private Long orderId;
}
