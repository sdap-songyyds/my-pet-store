package cn.edu.csu.mycoursestore.admin.dto;

import cn.edu.csu.mycoursestore.user.entity.UserRole;
import cn.edu.csu.mycoursestore.user.entity.UserStatus;

public record AdminUserFilter(String keyword, UserRole role, UserStatus status) {
}
