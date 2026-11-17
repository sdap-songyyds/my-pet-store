package cn.edu.csu.mycoursestore.security;

import cn.edu.csu.mycoursestore.user.entity.StoreUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserModelAdvice {

    @ModelAttribute("currentUser")
    public StoreUser currentUser(HttpServletRequest request) {
        return (StoreUser) request.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);
    }
}