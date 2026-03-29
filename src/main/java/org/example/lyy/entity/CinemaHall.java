package org.example.lyy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cinema_hall")
public class CinemaHall {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer rowCount;
    private Integer colCount;
    private String seatLayout;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}