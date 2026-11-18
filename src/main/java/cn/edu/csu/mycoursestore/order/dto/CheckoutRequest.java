package cn.edu.csu.mycoursestore.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @NotBlank(message = "收货人不能为空")
        @Size(max = 50, message = "收货人不能超过 50 个字符")
        String receiverName,

        @NotBlank(message = "联系电话不能为空")
        @Size(max = 30, message = "联系电话不能超过 30 个字符")
        String receiverPhone,

        @NotBlank(message = "收货地址不能为空")
        @Size(max = 255, message = "收货地址不能超过 255 个字符")
        String receiverAddress
) {
}