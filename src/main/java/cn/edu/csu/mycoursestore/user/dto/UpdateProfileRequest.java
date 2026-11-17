package cn.edu.csu.mycoursestore.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
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