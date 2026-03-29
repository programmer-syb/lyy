package org.example.lyy.controller.admin;

import org.example.lyy.annotation.RequireRole;
import org.example.lyy.common.Result;
import org.example.lyy.mapper.OrdersMapper;
import org.example.lyy.model.vo.BoxOfficeVO;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@RestController
@RequestMapping("/api/admin/sys/analysis")
@RequireRole({"SUPER_ADMIN", "MANAGER"}) // 仅限超级管理员和经理查看数据
public class SysDataAnalysisController {

    @Resource
    private OrdersMapper ordersMapper;

    /**
     * 1. 获取电影票房排行榜 (DA-02)
     * @param type 排行榜类型：daily (日榜), weekly (周榜), monthly (月榜), all (总榜)
     */
    @GetMapping("/box-office")
    public Result<List<BoxOfficeVO>> getBoxOfficeRanking(@RequestParam(defaultValue = "all") String type) {
        LocalDateTime startTime = null;
        LocalDateTime endTime = LocalDateTime.now(); // 默认截止到当前时间

        LocalDate today = LocalDate.now();
        switch (type.toLowerCase()) {
            case "daily":
                // 今天的起点
                startTime = LocalDateTime.of(today, LocalTime.MIN);
                break;
            case "weekly":
                // 本周一的起点
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                startTime = LocalDateTime.of(monday, LocalTime.MIN);
                break;
            case "monthly":
                // 本月1号的起点
                LocalDate firstDayOfMonth = today.withDayOfMonth(1);
                startTime = LocalDateTime.of(firstDayOfMonth, LocalTime.MIN);
                break;
            case "all":
            default:
                // 总榜：时间设为很久以前
                startTime = LocalDateTime.of(1970, 1, 1, 0, 0);
                break;
        }

        List<BoxOfficeVO> ranking = ordersMapper.getBoxOfficeRanking(startTime, endTime);
        return Result.success(ranking);
    }
}