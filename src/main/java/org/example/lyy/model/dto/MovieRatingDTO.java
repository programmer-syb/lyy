package org.example.lyy.model.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MovieRatingDTO {
    private Long movieId;
    private BigDecimal rating; // 1-5星
    private String comment;    // 评论内容
}