package cn.edu.csu.mycoursestore.order.dto;

import cn.edu.csu.mycoursestore.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public record OrderDetail(
        Long id,
        String orderNo,
        OrderStatus status,
        String statusLabel,
        BigDecimal totalAmount,
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
        List<OrderLine> lines,
        boolean canPay,
        boolean canCancel
) {
}