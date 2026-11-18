package cn.edu.csu.mycoursestore.admin.dto;

import cn.edu.csu.mycoursestore.user.entity.UserRole;
import cn.edu.csu.mycoursestore.user.entity.UserStatus;

public record AdminUserDetail(
        Long id,
        String username,
        String displayName,
        UserRole role,
        String roleLabel,
        UserStatus status,
        String statusLabel,
        String email,
        String phone,
        String createdAtText
) {
}
