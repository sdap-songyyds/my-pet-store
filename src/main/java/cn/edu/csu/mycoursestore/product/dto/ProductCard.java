package cn.edu.csu.mycoursestore.product.dto;

import java.math.BigDecimal;

public record ProductCard(
        Long id,
        String name,
        String summary,
        String coverImage,
        BigDecimal price,
        int stock
) {
}