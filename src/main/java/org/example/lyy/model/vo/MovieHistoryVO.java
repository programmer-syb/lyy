package org.example.lyy.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MovieHistoryVO {
    private Long movieId;
    private String title;
    private String posterUrl;
    private String actionType; // "购票" 或 "评分"
    private LocalDateTime actionTime; // 购票时间或评分时间
    private String details; // 附加信息，如：评分分数、或排片时间
}