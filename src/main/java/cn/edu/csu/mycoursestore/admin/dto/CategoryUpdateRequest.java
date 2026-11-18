package cn.edu.csu.mycoursestore.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryUpdateRequest(
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 50, message = "分类名称不能超过 50 个字符")
        String name,

        @NotNull(message = "排序不能为空")
        @Min(value = 0, message = "排序不能小于 0")
        Integer sortOrder
) {
}
