package org.example.lyy.model.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ScheduleVO {
    private Long scheduleId;
    private Long movieId;
    private Long hallId;
    private String hallName; // 关联的影厅名称
    private LocalDate showDate;
    private LocalTime showTime;
    private BigDecimal price;
}