package org.example.lyy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("movie_rating")
public class MovieRating {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long movieId;
    private BigDecimal rating;
    private String comment;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}