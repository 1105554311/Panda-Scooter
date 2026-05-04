package com.panda.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AddPackageDTO {
    private String title;
    private String description;
    private BigDecimal price;
    private Integer type;
    private String createTime;
}