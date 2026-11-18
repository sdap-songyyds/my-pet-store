package cn.edu.csu.mycoursestore.admin.dto;

import cn.edu.csu.mycoursestore.order.entity.OrderStatus;

import java.time.LocalDate;

public record AdminOrderFilter(
        String keyword,
        OrderStatus status,
        LocalDate from,
        LocalDate to
) {
}
