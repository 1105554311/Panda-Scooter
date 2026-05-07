package com.panda.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AddDispatcherVO {
    private Long id;
    private String name;
    private String email;
    private Long areaId;
    private LocalDateTime createTime;
}