package cn.edu.csu.mycoursestore.admin.dto;

import cn.edu.csu.mycoursestore.product.entity.ProductStatus;

import java.math.BigDecimal;

public record AdminInventoryRow(
        Long id,
        String sku,
        String name,
        String categoryPath,
        String coverImage,
        BigDecimal price,
        int stock,
        ProductStatus status,
        String statusLabel,
        long version
) {
}
