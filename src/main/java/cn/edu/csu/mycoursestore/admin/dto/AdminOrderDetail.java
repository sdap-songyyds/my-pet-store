package cn.edu.csu.mycoursestore.admin.dto;

import cn.edu.csu.mycoursestore.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public record AdminOrderDetail(
        Long id,
        String orderNo,
        Long userId,
        String username,
        String displayName,
        OrderStatus status,
        String statusLabel,
        BigDecimal totalAmount,
        int totalQuantity,
        String receiverName,
        String receiverPhone,
        String receiverAddress,
        String paymentMethod,
        String paymentNo,
        String createdAtText,
        String paidAtText,
        String cancelledAtText,
        String shippedAtText,
        String completedAtText,
        List<AdminOrderLine> lines,
        boolean canCancel,
        boolean canShip,
        boolean canComplete,
        boolean canDelete
) {
}
