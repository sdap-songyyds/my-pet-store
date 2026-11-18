package cn.edu.csu.mycoursestore.cart.dto;

import java.math.BigDecimal;

public record CartLine(
        Long productId,
        String name,
        String coverImage,
        BigDecimal price,
        int stock,
        int quantity,
        int maxQuantity,
        BigDecimal lineAmount,
        boolean onSale,
        boolean stockEnough
) {
}