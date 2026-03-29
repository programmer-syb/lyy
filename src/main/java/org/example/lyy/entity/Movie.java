package org.example.lyy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("movie")
public class Movie {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String genres;
    private String director;
    private String actors;
    private Integer duration;
    private String introduction;
    private String posterUrl;
    private String trailerUrl;
    private BigDecimal globalRating;
    private Integer ratingCount;
    private Integer heat;
    private LocalDate releaseDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}