package org.example.lyy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.lyy.common.Result;
import org.example.lyy.entity.CinemaHall;
import org.example.lyy.entity.Orders;
import org.example.lyy.entity.Schedule;
import org.example.lyy.mapper.ScheduleMapper;
import org.example.lyy.model.vo.ScheduleVO;
import org.example.lyy.model.vo.SeatMapVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    @Resource
    private ScheduleMapper scheduleMapper;
    @Resource
    private org.example.lyy.service.CinemaHallService cinemaHallService;
    @Resource
    private org.example.lyy.service.OrdersService orderService;
    @Resource
    private org.example.lyy.service.ScheduleService scheduleService;

    /**
     * 根据电影ID和日期查询排片场次 (MV-04)
     * 示例请求: /api/schedule/list?movieId=1&showDate=2023-11-01
     */
    @GetMapping("/list")
    public Result<List<ScheduleVO>> getScheduleList(
            @RequestParam Long movieId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate showDate) {

        List<ScheduleVO> list = scheduleMapper.selectScheduleWithHall(movieId, showDate);
        // 按时间早晚排序
        list.sort((a, b) -> a.getShowTime().compareTo(b.getShowTime()));

        return Result.success(list);
    }

    /**
     * 获取指定排片场次的实时座位图 (MV-05)
     */
    @GetMapping("/{scheduleId}/seats")
    public Result<SeatMapVO> getSeatMap(@PathVariable Long scheduleId) {
        // 1. 获取场次和影厅信息
        Schedule schedule = scheduleService.getById(scheduleId);
        if (schedule == null) {
            return Result.error("排片场次不存在");
        }
        CinemaHall hall = cinemaHallService.getById(schedule.getHallId());

        // 2. 查询该场次下所有的有效订单 (status 0:待支付锁定中, 1:已支付已售出)
        LambdaQueryWrapper<Orders> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Orders::getScheduleId, scheduleId)
                .in(Orders::getStatus, 0, 1);
        List<Orders> activeOrders = orderService.list(orderWrapper);

        List<String> soldSeats = new ArrayList<>();
        List<String> lockedSeats = new ArrayList<>();

        // 3. 解析订单中的座位信息
        // 假设数据库中 seats 字段存储格式为逗号分隔，如 "1-1,1-2" (第1排1座，第1排2座)
        for (Orders order : activeOrders) {
            String[] seatArr = order.getSeats().split(",");
            if (order.getStatus() == 1) {
                soldSeats.addAll(Arrays.asList(seatArr));
            } else if (order.getStatus() == 0) {
                lockedSeats.addAll(Arrays.asList(seatArr));
            }
        }

        // 4. 组装数据返回给前端渲染
        SeatMapVO seatMapVO = new SeatMapVO();
        seatMapVO.setRowCount(hall.getRowCount());
        seatMapVO.setColCount(hall.getColCount());
        seatMapVO.setSeatLayout(hall.getSeatLayout()); // 如果是原生 JSON 字段可以直接转
        seatMapVO.setSoldSeats(soldSeats);
        seatMapVO.setLockedSeats(lockedSeats);

        return Result.success(seatMapVO);
    }
}