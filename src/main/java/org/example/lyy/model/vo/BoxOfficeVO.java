package org.example.lyy.model.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BoxOfficeVO {
    private Long movieId;
    private String title;
    private String posterUrl;
    private BigDecimal boxOffice; // 总票房金额
}