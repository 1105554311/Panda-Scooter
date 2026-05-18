package com.panda.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WechatPayCallbackDTO {

    @NotNull
    private Long orderId;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal amount;

    private String transactionId;

    private String tradeState;
}
