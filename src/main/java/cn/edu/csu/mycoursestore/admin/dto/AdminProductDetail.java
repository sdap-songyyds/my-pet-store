package cn.edu.csu.mycoursestore.admin.dto;

import cn.edu.csu.mycoursestore.product.entity.ProductStatus;

import java.math.BigDecimal;
import java.util.List;

public record AdminProductDetail(
        Long id,
        String sku,
        Long categoryId,
        String categoryPath,
        String name,
        String summary,
        String description,
        String coverImage,
        BigDecimal price,
        int stock,
        ProductStatus status,
        String statusLabel,
        int sortOrder,
        long version,
        String createdAtText,
        String updatedAtText,
        List<GalleryImage> images
) {

    public record GalleryImage(Long id, String imagePath, String altText, int sortOrder) {
    }
}
