package cn.edu.csu.mycoursestore.admin.dto;

public record AdminCategoryRow(
        Long id,
        String code,
        String name,
        Long parentId,
        int sortOrder,
        int childCount,
        int productCount,
        String deleteBlockedReason
) {
}
