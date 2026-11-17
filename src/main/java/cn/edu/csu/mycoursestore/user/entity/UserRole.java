package cn.edu.csu.mycoursestore.user.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserRole {

    CUSTOMER("CUSTOMER"),
    ADMIN("ADMIN");

    @EnumValue
    private final String value;

    UserRole(String value) {
        this.value = value;
    }
}