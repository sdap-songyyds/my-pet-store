package cn.edu.csu.mycoursestore.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注后台入口：必须已登录，且数据库中的角色为 ADMIN。
 * 未登录按 {@link RequireAuth} 的方式跳转/返回 401，非 ADMIN 返回 403。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {
}
