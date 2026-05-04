package com.panda.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PricingRuleEditDTO {
    private BigDecimal pricePerMin;   // 每分钟单价
    private BigDecimal basePrice;     // 起步价
    private Integer billingInterval;  // 计费间隔（分钟）
}