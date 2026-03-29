package org.example.lyy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.lyy.common.Result;
import org.example.lyy.entity.Orders;
import org.example.lyy.model.dto.OrderCreateDTO;
import org.example.lyy.service.OrdersService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Resource
    private OrdersService ordersService;

    /**
     * 1. 选座下单 (OR-01)
     */
    @PostMapping("/create")
    public Result<Orders> createOrder(@RequestBody OrderCreateDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        Orders order = ordersService.createOrder(userId, dto);
        return Result.success(order);
    }

    /**
     * 2. 模拟支付 (OR-02)
     */
    @PostMapping("/pay/{orderId}")
    public Result<String> payOrder(@PathVariable Long orderId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        ordersService.payOrder(userId, orderId);
        return Result.success("支付成功");
    }

    /**
     * 3. 主动取消订单 (支持 OR-03)
     */
    @PostMapping("/cancel/{orderId}")
    public Result<String> cancelOrder(@PathVariable Long orderId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        ordersService.cancelOrder(userId, orderId);
        return Result.success("订单已取消，座位已释放");
    }

    /**
     * 4. 查看已购影票 (OR-04)
     * 展示已支付订单，前端可直接从中提取 ticketCode 取票码
     */
    @GetMapping("/purchased")
    public Result<List<Orders>> getPurchasedTickets(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");

        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        // 状态 1 代表已支付
        wrapper.eq(Orders::getUserId, userId)
                .eq(Orders::getStatus, 1)
                .orderByDesc(Orders::getCreateTime);

        List<Orders> list = ordersService.list(wrapper);
        return Result.success(list);
    }

    /**
     * 5. 历史订单查询 (OR-05)
     * 支持分页，且支持按订单状态过滤
     */
    @GetMapping("/history")
    public Result<Page<Orders>> getHistoryOrders(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status, // 可选：传入 0, 1, 2 进行过滤
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("currentUserId");
        Page<Orders> page = new Page<>(current, size);

        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId);

        // 如果前端传了 status，就按状态过滤
        if (status != null) {
            wrapper.eq(Orders::getStatus, status);
        }

        // 按时间倒序
        wrapper.orderByDesc(Orders::getCreateTime);

        Page<Orders> resultPage = ordersService.page(page, wrapper);
        return Result.success(resultPage);
    }
}