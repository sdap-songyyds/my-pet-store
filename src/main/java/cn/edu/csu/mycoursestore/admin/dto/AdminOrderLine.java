package cn.edu.csu.mycoursestore.admin.dto;

import java.math.BigDecimal;

public record AdminOrderLine(
        Long productId,
        String sku,
        String name,
        String coverImage,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineAmount
) {
}
