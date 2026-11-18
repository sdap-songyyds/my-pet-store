package cn.edu.csu.mycoursestore.admin.dto;

public record AdminCategoryDetail(
        Long id,
        String code,
        String name,
        Long parentId,
        String parentName,
        int sortOrder
) {
}
