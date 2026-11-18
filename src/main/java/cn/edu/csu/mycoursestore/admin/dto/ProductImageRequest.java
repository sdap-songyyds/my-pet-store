package cn.edu.csu.mycoursestore.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductImageRequest(
        @NotBlank(message = "图片路径不能为空")
        @Size(max = 255, message = "图片路径不能超过 255 个字符")
        String imagePath,

        @Size(max = 120, message = "替代文本不能超过 120 个字符")
        String altText,

        @NotNull(message = "排序不能为空")
        @Min(value = 0, message = "排序不能小于 0")
        Integer sortOrder
) {
}
