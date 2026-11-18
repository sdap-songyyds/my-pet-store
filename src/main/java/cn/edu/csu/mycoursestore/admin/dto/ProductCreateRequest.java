package cn.edu.csu.mycoursestore.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank(message = "SKU 不能为空")
        @Size(max = 40, message = "SKU 不能超过 40 个字符")
        String sku,

        @NotNull(message = "请选择二级分类")
        Long categoryId,

        @NotBlank(message = "商品名称不能为空")
        @Size(max = 120, message = "商品名称不能超过 120 个字符")
        String name,

        @Size(max = 500, message = "简介不能超过 500 个字符")
        String summary,

        String description,

        @NotBlank(message = "封面路径不能为空")
        @Size(max = 255, message = "封面路径不能超过 255 个字符")
        String coverImage,

        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0.01", message = "价格必须大于 0")
        @Digits(integer = 10, fraction = 2, message = "价格最多 10 位整数、2 位小数")
        BigDecimal price,

        @NotNull(message = "库存不能为空")
        @Min(value = 0, message = "库存不能小于 0")
        @Max(value = 2147483647, message = "库存超出范围")
        Integer stock,

        @NotNull(message = "排序不能为空")
        @Min(value = 0, message = "排序不能小于 0")
        Integer sortOrder
) {
}
