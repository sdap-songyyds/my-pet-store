package cn.edu.csu.mycoursestore.order.dto;

import java.math.BigDecimal;

public record OrderLine(
        Long productId,
        String sku,
        String name,
        String coverImage,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineAmount
) {
}