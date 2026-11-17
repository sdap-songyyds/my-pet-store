package cn.edu.csu.mycoursestore.user.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserStatus {

    ENABLED("ENABLED"),
    DISABLED("DISABLED");

    @EnumValue
    private final String value;

    UserStatus(String value) {
        this.value = value;
    }
}