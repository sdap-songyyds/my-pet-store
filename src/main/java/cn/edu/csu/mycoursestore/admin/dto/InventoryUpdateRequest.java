package cn.edu.csu.mycoursestore.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryUpdateRequest(
        @NotNull(message = "库存不能为空")
        @Min(value = 0, message = "库存不能小于 0")
        @Max(value = 2147483647, message = "库存超出范围")
        Integer stock,

        @NotNull(message = "商品版本缺失，请刷新后重试")
        @Min(value = 0, message = "商品版本不合法")
        Long version
) {
}
