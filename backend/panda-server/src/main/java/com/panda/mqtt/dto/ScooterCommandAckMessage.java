package com.panda.mqtt.dto;

import lombok.Data;

@Data
public class ScooterCommandAckMessage {

    private String commandId;

    private String command;

    private Long orderId;

    private Boolean success;

    private String message;

    private Long timestamp;
}
