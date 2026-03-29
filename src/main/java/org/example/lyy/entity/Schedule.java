package org.example.lyy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("schedule")
public class Schedule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long movieId;
    private Long hallId;
    private LocalDate showDate;
    private LocalTime showTime;
    private BigDecimal price;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}