package org.example.lyy.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.lyy.annotation.RequireRole;
import org.example.lyy.common.Result;
import org.example.lyy.entity.Movie;
import org.example.lyy.entity.Orders;
import org.example.lyy.entity.Schedule;
import org.example.lyy.service.MovieService;
import org.example.lyy.service.OrdersService;
import org.example.lyy.service.ScheduleService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sys/schedule")
@RequireRole({"SUPER_ADMIN", "MANAGER"})
public class SysScheduleController {

    @Resource
    private ScheduleService scheduleService;
    @Resource
    private MovieService movieService;
    @Resource
    private OrdersService ordersService;

    // 1. 新增排片
    @PostMapping("/add")
    public Result<String> addSchedule(@RequestBody Schedule schedule) {
        checkConflict(schedule);
        scheduleService.save(schedule);
        return Result.success("排片添加成功");
    }

    // 2. 批量新增排片 (带冲突校验)
    @PostMapping("/addBatch")
    @Transactional(rollbackFor = Exception.class) // 开启事务，一个冲突全部回滚
    public Result<String> addBatchSchedule(@RequestBody List<Schedule> schedules) {
        for (Schedule schedule : schedules) {
            checkConflict(schedule); // 逐个校验
        }
        scheduleService.saveBatch(schedules);
        return Result.success("批量排片成功，共添加 " + schedules.size() + " 场");
    }

    // 3. 修改排片 (比如改票价、改时间)
    @PutMapping("/update")
    public Result<String> updateSchedule(@RequestBody Schedule schedule) {
        scheduleService.updateById(schedule);
        return Result.success("排片修改成功");
    }

    // 4. 删除排片 (注意：如果该场次已经有人买票，理论上不允许删除，需要退票逻辑。这里做基础删除)
    @DeleteMapping("/delete/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> deleteSchedule(@PathVariable Long id) {
        // 查询该场次下是否有已支付(1)或待支付(0)的订单
        LambdaQueryWrapper<Orders> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Orders::getScheduleId, id).in(Orders::getStatus, 0, 1);
        List<Orders> activeOrders = ordersService.list(orderWrapper);

        if (!activeOrders.isEmpty()) {
            // 理论上这里需要对接微信/支付宝的退款 API
            // 我们这里做业务上的模拟退款：将这些订单的状态统一修改为 3 (已退款/排片取消)
            for (Orders order : activeOrders) {
                order.setStatus(3);
            }
            ordersService.updateBatchById(activeOrders);
        }

        // 删除排片
        scheduleService.removeById(id);
        return Result.success(activeOrders.isEmpty() ? "排片取消成功" : "排片取消成功，并已为 " + activeOrders.size() + " 个订单办理退票");
    }

    @GetMapping("/page")
    public Result<Page<Schedule>> pageSchedules(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long hallId,
            @RequestParam(required = false) String showDate) {

        Page<Schedule> page = new Page<>(current, size);
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();

        // 修复点：使用 hasText 过滤掉空字符串 ""
        if (movieId != null) wrapper.eq(Schedule::getMovieId, movieId);
        if (hallId != null) wrapper.eq(Schedule::getHallId, hallId);
        if (StringUtils.hasText(showDate)) wrapper.eq(Schedule::getShowDate, showDate);

        // 按放映日期和时间排序
        wrapper.orderByDesc(Schedule::getShowDate).orderByAsc(Schedule::getShowTime);

        return Result.success(scheduleService.page(page, wrapper));
    }

    private void checkConflict(Schedule schedule) {
        Movie newMovie = movieService.getById(schedule.getMovieId());
        if (newMovie == null || newMovie.getDuration() == null) {
            throw new RuntimeException("电影ID为 " + schedule.getMovieId() + " 的信息不存在或缺失时长数据");
        }

        java.time.LocalTime newStart = schedule.getShowTime();
        java.time.LocalTime newEnd = newStart.plusMinutes(newMovie.getDuration() + 15);

        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedule::getHallId, schedule.getHallId())
                .eq(Schedule::getShowDate, schedule.getShowDate());
        List<Schedule> existingSchedules = scheduleService.list(wrapper);

        for (Schedule ext : existingSchedules) {
            Movie extMovie = movieService.getById(ext.getMovieId());
            int extDuration = (extMovie != null && extMovie.getDuration() != null) ? extMovie.getDuration() : 120;
            java.time.LocalTime extStart = ext.getShowTime();
            java.time.LocalTime extEnd = extStart.plusMinutes(extDuration + 15);

            if (newStart.isBefore(extEnd) && newEnd.isAfter(extStart)) {
                throw new RuntimeException("排片冲突！影厅ID " + schedule.getHallId() + " 在 " + extStart + " - " + extEnd + " 已有排片");
            }
        }
    }
}