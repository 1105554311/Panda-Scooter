package com.panda.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PricingRuleVO {
    private Long id;
    private BigDecimal pricePerMin;
    private BigDecimal basePrice;
    private Integer billingInterval;
}