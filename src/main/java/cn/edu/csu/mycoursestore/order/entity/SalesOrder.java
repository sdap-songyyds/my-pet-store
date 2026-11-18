package cn.edu.csu.mycoursestore.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sales_order")
public class SalesOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private OrderStatus status;

    private BigDecimal totalAmount;

    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String paymentMethod;

    private String paymentNo;

    private LocalDateTime paidAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime shippedAt;

    private LocalDateTime completedAt;
}