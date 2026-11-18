package cn.edu.csu.mycoursestore.order.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum OrderStatus {

    PENDING_PAYMENT("PENDING_PAYMENT", "待支付"),
    PAID("PAID", "已支付"),
    SHIPPED("SHIPPED", "已发货"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    @EnumValue
    private final String value;

    private final String label;

    OrderStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }
}