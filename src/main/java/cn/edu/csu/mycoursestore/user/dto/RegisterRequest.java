package cn.edu.csu.mycoursestore.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_]{3,30}$", message = "用户名只能包含字母、数字、下划线，长度 3-30")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度需为 8-72 个字符")
        String password,

        @NotBlank(message = "显示名称不能为空")
        @Size(max = 50, message = "显示名称不能超过 50 个字符")
        String displayName,

        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱不能超过 100 个字符")
        String email,

        @Size(max = 30, message = "手机号不能超过 30 个字符")
        String phone
) {
}