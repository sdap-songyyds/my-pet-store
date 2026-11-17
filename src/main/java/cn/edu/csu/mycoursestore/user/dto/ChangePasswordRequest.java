package cn.edu.csu.mycoursestore.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "当前密码不能为空")
        String currentPassword,

        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 72, message = "新密码长度需为 8-72 个字符")
        String newPassword,

        @NotBlank(message = "请再次输入新密码")
        String confirmPassword
) {
}