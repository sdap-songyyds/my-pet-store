package cn.edu.csu.mycoursestore.order.dto;

import cn.edu.csu.mycoursestore.order.entity.OrderStatus;

import java.math.BigDecimal;

public record OrderSummary(
        Long id,
        String orderNo,
        OrderStatus status,
        String statusLabel,
        BigDecimal totalAmount,
        int totalQuantity,
        String createdAtText
) {
}