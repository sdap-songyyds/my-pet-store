package cn.edu.csu.mycoursestore.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetail(
        Long id,
        String name,
        String summary,
        String description,
        String coverImage,
        BigDecimal price,
        int stock,
        String categoryPath,
        List<GalleryImage> images
) {

    public record GalleryImage(String path, String alt) {
    }
}