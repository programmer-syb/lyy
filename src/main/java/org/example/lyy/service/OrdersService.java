package org.example.lyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.lyy.entity.Orders;
import org.example.lyy.model.dto.OrderCreateDTO;

public interface OrdersService extends IService<Orders> {
    Orders createOrder(Long userId, OrderCreateDTO dto);
    void payOrder(Long userId, Long orderId);
    void cancelOrder(Long userId, Long orderId);
}