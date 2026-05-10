package com.panda.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScooterCommand {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_SENT = 1;
    public static final int STATUS_ACKED = 2;
    public static final int STATUS_FAILED = 3;
    public static final int STATUS_TIMEOUT = 4;

    private Long id;
    private String commandId;
    private Long scooterId;
    private String scooterCode;
    private Long orderId;
    private String commandType;
    private String payload;
    private Integer status;
    private Integer retryCount;
    private Integer maxRetryCount;
    private String errorMessage;
    private LocalDateTime sendTime;
    private LocalDateTime ackTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
