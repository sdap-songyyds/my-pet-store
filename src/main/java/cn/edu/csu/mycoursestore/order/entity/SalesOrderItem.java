package cn.edu.csu.mycoursestore.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("sales_order_item")
public class SalesOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long productId;

    private String skuSnapshot;

    private String productNameSnapshot;

    private String coverImageSnapshot;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal lineAmount;
}