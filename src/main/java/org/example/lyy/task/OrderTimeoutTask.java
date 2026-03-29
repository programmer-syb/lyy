package org.example.lyy.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.lyy.entity.Orders;
import org.example.lyy.service.OrdersService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

@Component
public class OrderTimeoutTask {

    @Resource
    private OrdersService ordersService;

    /**
     * 每分钟执行一次，扫描并取消超时订单 (OR-03)
     * cron = "0 * * * * ?" 表示每分钟的第0秒执行
     */
    @Scheduled(cron = "0 * * * * ?")
    public void cancelTimeoutOrders() {
        // 计算 15 分钟前的时间节点
        LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(15);

        LambdaUpdateWrapper<Orders> updateWrapper = new LambdaUpdateWrapper<>();
        // 条件：状态为待支付 (0) 且 创建时间 <= 15分钟前
        updateWrapper.eq(Orders::getStatus, 0)
                     .le(Orders::getCreateTime, thresholdTime)
                     // 动作：将状态修改为已取消 (2)
                     .set(Orders::getStatus, 2);

        // 执行批量更新
        boolean result = ordersService.update(updateWrapper);
        // 你可以在这里加个 log.info() 打印取消了多少条订单
    }
}