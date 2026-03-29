package org.example.lyy.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.lyy.entity.Orders;
import org.example.lyy.entity.Schedule;
import org.example.lyy.mapper.OrdersMapper;
import org.example.lyy.model.dto.OrderCreateDTO;
import org.example.lyy.service.OrdersService;
import org.example.lyy.service.ScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

    @Resource
    private ScheduleService scheduleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Orders createOrder(Long userId, OrderCreateDTO dto) {
        // 1. 获取排片信息
        Schedule schedule = scheduleService.getById(dto.getScheduleId());
        if (schedule == null) {
            throw new RuntimeException("场次不存在");
        }

        // 2. 防超卖校验：检查选中的座位是否已被占用 (status 0 或 1)
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getScheduleId, dto.getScheduleId())
                .in(Orders::getStatus, 0, 1);
        List<Orders> existingOrders = this.list(wrapper);

        for (Orders order : existingOrders) {
            List<String> existingSeats = Arrays.asList(order.getSeats().split(","));
            for (String seat : dto.getSeats()) {
                if (existingSeats.contains(seat)) {
                    throw new RuntimeException("手慢了，座位 " + seat + " 已被锁定或售出");
                }
            }
        }

        // 3. 计算总价并生成订单
        Orders order = new Orders();
        // 使用 Hutool 生成雪花算法流水号
        order.setOrderNo(IdUtil.getSnowflakeNextIdStr());
        order.setUserId(userId);
        order.setScheduleId(schedule.getId());
        order.setSeats(String.join(",", dto.getSeats())); // 存入数据库变为 "1-2,1-3"
        order.setTotalPrice(schedule.getPrice().multiply(new BigDecimal(dto.getSeats().size())));
        order.setStatus(0); // 0: 待支付
        order.setExpireTime(LocalDateTime.now().plusMinutes(15)); // 设置15分钟过期时间

        this.save(order);

        // TODO: 在这里向 RabbitMQ 的延迟/死信队列发送消息 (投递 order.getId())

        return order;
    }

    @Override
    public void payOrder(Long userId, Long orderId) {
        Orders order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("订单异常");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态不允许支付");
        }

        // 模拟支付成功，更新状态并生成取票码
        order.setStatus(1); // 1: 已支付
        order.setTicketCode(RandomUtil.randomNumbers(8)); // 随机生成 8 位纯数字取票码
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, Long orderId) {
        Orders order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("订单不存在或无权操作");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("只能取消待支付状态的订单");
        }

        // 修改状态为已取消 (2)，一旦状态改变，之前锁定的座位在座位图接口中就会被释放
        order.setStatus(2);
        this.updateById(order);
    }
}