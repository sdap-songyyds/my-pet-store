package cn.edu.csu.mycoursestore.user.dto;

import cn.edu.csu.mycoursestore.user.entity.StoreUser;
import cn.edu.csu.mycoursestore.user.entity.UserRole;

public record UserProfile(
        Long id,
        String username,
        String displayName,
        UserRole role,
        String email,
        String phone
) {

    public static UserProfile from(StoreUser user) {
        return new UserProfile(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getRole(), user.getEmail(), user.getPhone());
    }
}