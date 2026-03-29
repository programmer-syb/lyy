package org.example.lyy.model.vo;

import lombok.Data;
import java.util.List;

@Data
public class SeatMapVO {
    private Integer rowCount;
    private Integer colCount;
    private String seatLayout;       // 影厅本身的布局(如走道、残损座位)，JSON字符串
    private List<String> soldSeats;  // 已售出的座位列表，如 ["1-1", "1-2"]
    private List<String> lockedSeats;// 锁定中(待付款)的座位列表
}