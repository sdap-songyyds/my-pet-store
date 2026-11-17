package cn.edu.csu.mycoursestore.product.dto;

import java.util.List;

public record CategoryNode(
        Long id,
        String name,
        List<CategoryNode> children
) {
}