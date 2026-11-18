package cn.edu.csu.mycoursestore.admin.dto;

import cn.edu.csu.mycoursestore.product.entity.ProductStatus;

public record AdminProductFilter(String keyword, Long categoryId, ProductStatus status) {
}
