package cn.edu.csu.mycoursestore.admin.dto;

import cn.edu.csu.mycoursestore.order.entity.OrderStatus;

import java.math.BigDecimal;

public record AdminOrderRow(
        Long id,
        String orderNo,
        Long userId,
        String username,
        String displayName,
        OrderStatus status,
        String statusLabel,
        BigDecimal totalAmount,
        int totalQuantity,
        String createdAtText
) {
}
