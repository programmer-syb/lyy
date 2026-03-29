package org.example.lyy.model.dto;
import lombok.Data;
import java.util.List;

@Data
public class OrderCreateDTO {
    private Long scheduleId;
    private List<String> seats; // 用户选中的座位列表，例如 ["1-2", "1-3"]
}