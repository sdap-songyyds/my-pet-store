package cn.edu.csu.mycoursestore.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank(message = "分类编码不能为空")
        @Size(max = 32, message = "分类编码不能超过 32 个字符")
        String code,

        Long parentId,

        @NotBlank(message = "分类名称不能为空")
        @Size(max = 50, message = "分类名称不能超过 50 个字符")
        String name,

        @NotNull(message = "排序不能为空")
        @Min(value = 0, message = "排序不能小于 0")
        Integer sortOrder
) {
}
