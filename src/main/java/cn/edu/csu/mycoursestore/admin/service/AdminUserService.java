package cn.edu.csu.mycoursestore.admin.service;

import cn.edu.csu.mycoursestore.admin.dto.AdminPasswordResetRequest;
import cn.edu.csu.mycoursestore.admin.dto.AdminUserDetail;
import cn.edu.csu.mycoursestore.admin.dto.AdminUserFilter;
import cn.edu.csu.mycoursestore.admin.dto.AdminUserRow;
import cn.edu.csu.mycoursestore.admin.paging.PageQuery;
import cn.edu.csu.mycoursestore.admin.paging.PageResult;
import cn.edu.csu.mycoursestore.cart.entity.CartItem;
import cn.edu.csu.mycoursestore.cart.mapper.CartItemMapper;
import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.order.entity.SalesOrder;
import cn.edu.csu.mycoursestore.order.mapper.SalesOrderMapper;
import cn.edu.csu.mycoursestore.user.entity.StoreUser;
import cn.edu.csu.mycoursestore.user.entity.UserRole;
import cn.edu.csu.mycoursestore.user.entity.UserStatus;
import cn.edu.csu.mycoursestore.user.mapper.StoreUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final StoreUserMapper userMapper;
    private final SalesOrderMapper orderMapper;
    private final CartItemMapper cartItemMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(StoreUserMapper userMapper,
                            SalesOrderMapper orderMapper,
                            CartItemMapper cartItemMapper,
                            PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.orderMapper = orderMapper;
        this.cartItemMapper = cartItemMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<AdminUserRow> list(AdminUserFilter filter, PageQuery page, long currentUserId) {
        long total = userMapper.selectCount(buildWrapper(filter));
        if (total == 0) {
            return PageResult.empty(page);
        }

        List<StoreUser> users = userMapper.selectList(buildWrapper(filter)
                .orderByAsc(StoreUser::getId)
                .last("LIMIT " + page.size() + " OFFSET " + page.offset()));
        List<Long> userIds = users.stream().map(StoreUser::getId).toList();
        Map<Long, Long> orderCounts = orderCounts(userIds);
        Map<Long, Long> cartCounts = cartCounts(userIds);

        List<AdminUserRow> rows = users.stream()
                .map(user -> {
                    long orders = orderCounts.getOrDefault(user.getId(), 0L);
                    return new AdminUserRow(
                            user.getId(),
                            user.getUsername(),
                            user.getDisplayName(),
                            user.getRole(),
                            roleLabel(user.getRole()),
                            user.getStatus(),
                            statusLabel(user.getStatus()),
                            user.getEmail(),
                            user.getPhone(),
                            format(user.getCreatedAt()),
                            orders,
                            cartCounts.getOrDefault(user.getId(), 0L),
                            deleteBlockedReason(user, currentUserId, orders));
                })
                .toList();
        return new PageResult<>(rows, page.page(), page.size(), total);
    }

    public AdminUserDetail detail(long id) {
        StoreUser user = requireUser(id);
        return new AdminUserDetail(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                roleLabel(user.getRole()),
                user.getStatus(),
                statusLabel(user.getStatus()),
                user.getEmail(),
                user.getPhone(),
                format(user.getCreatedAt()));
    }

    @Transactional
    public void delete(long id, long currentUserId) {
        StoreUser user = requireUser(id);
        if (id == currentUserId) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_SELF_DELETE_FORBIDDEN",
                    "不能删除当前登录账号");
        }
        if (user.getRole() == UserRole.ADMIN) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_ADMIN_DELETE_FORBIDDEN",
                    "管理员账号不可删除");
        }

        long orders = orderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getUserId, id));
        if (orders > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_HAS_ORDERS",
                    "该用户有 " + orders + " 笔订单，不能删除");
        }

        try {
            cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, id));
            if (userMapper.deleteById(id) == 0) {
                throw deleteConflict();
            }
        } catch (DataIntegrityViolationException e) {
            throw deleteConflict();
        }
    }

    @Transactional
    public String updateStatus(long id, UserStatus target, UserStatus expectedStatus, long currentUserId) {
        StoreUser user = requireUser(id);
        if (target == null || expectedStatus == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "用户状态不合法");
        }
        if (target == UserStatus.DISABLED && id == currentUserId) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_DISABLE_SELF_FORBIDDEN",
                    "不能禁用当前登录的管理员账号");
        }
        if (user.getRole() == UserRole.ADMIN) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_ADMIN_STATUS_FORBIDDEN",
                    "管理员账号不可启用或禁用");
        }
        if (user.getStatus() == target) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_STATUS_UNCHANGED",
                    target == UserStatus.DISABLED ? "用户已是禁用状态" : "用户已是启用状态");
        }

        int rows = userMapper.update(null, new LambdaUpdateWrapper<StoreUser>()
                .eq(StoreUser::getId, id)
                .eq(StoreUser::getStatus, expectedStatus)
                .set(StoreUser::getStatus, target)
                .setSql("updated_at = LOCALTIMESTAMP"));
        if (rows == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_STATUS_CHANGED",
                    "用户状态已变化，请刷新后重试");
        }
        return user.getDisplayName();
    }

    @Transactional
    public void resetPassword(long id, AdminPasswordResetRequest request) {
        requireUser(id);
        int rows = userMapper.update(null, new LambdaUpdateWrapper<StoreUser>()
                .eq(StoreUser::getId, id)
                .set(StoreUser::getPasswordHash, passwordEncoder.encode(request.newPassword()))
                .set(StoreUser::getUpdatedAt, LocalDateTime.now()));
        if (rows == 0) {
            throw notFound();
        }
    }

    private LambdaQueryWrapper<StoreUser> buildWrapper(AdminUserFilter filter) {
        LambdaQueryWrapper<StoreUser> wrapper = new LambdaQueryWrapper<>();
        if (filter.role() != null) {
            wrapper.eq(StoreUser::getRole, filter.role());
        }
        if (filter.status() != null) {
            wrapper.eq(StoreUser::getStatus, filter.status());
        }
        String keyword = trimToNull(filter.keyword());
        if (keyword != null) {
            wrapper.and(inner -> inner.like(StoreUser::getUsername, keyword)
                    .or()
                    .like(StoreUser::getDisplayName, keyword)
                    .or()
                    .like(StoreUser::getEmail, keyword)
                    .or()
                    .like(StoreUser::getPhone, keyword));
        }
        return wrapper;
    }

    private Map<Long, Long> orderCounts(List<Long> userIds) {
        return orderMapper.selectList(new LambdaQueryWrapper<SalesOrder>()
                        .select(SalesOrder::getUserId)
                        .in(SalesOrder::getUserId, userIds))
                .stream()
                .collect(Collectors.groupingBy(SalesOrder::getUserId, Collectors.counting()));
    }

    private Map<Long, Long> cartCounts(List<Long> userIds) {
        return cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                        .select(CartItem::getUserId)
                        .in(CartItem::getUserId, userIds))
                .stream()
                .collect(Collectors.groupingBy(CartItem::getUserId, Collectors.counting()));
    }

    private StoreUser requireUser(long id) {
        StoreUser user = userMapper.selectById(id);
        if (user == null) {
            throw notFound();
        }
        return user;
    }

    private static String deleteBlockedReason(StoreUser user, long currentUserId, long orderCount) {
        if (user.getId() == currentUserId) {
            return "不能删除当前登录账号";
        }
        if (user.getRole() == UserRole.ADMIN) {
            return "管理员账号不可删除";
        }
        if (orderCount > 0) {
            return "该用户有 " + orderCount + " 笔订单";
        }
        return null;
    }

    private static String roleLabel(UserRole role) {
        if (role == null) {
            return "未知";
        }
        return switch (role) {
            case CUSTOMER -> "顾客";
            case ADMIN -> "管理员";
        };
    }

    private static String statusLabel(UserStatus status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case ENABLED -> "启用";
            case DISABLED -> "禁用";
        };
    }

    private static String format(LocalDateTime time) {
        return time == null ? "-" : time.format(TIME_FORMAT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "用户不存在");
    }

    private static ApiException deleteConflict() {
        return new ApiException(HttpStatus.CONFLICT, "USER_DELETE_CONFLICT", "用户状态已变化，请重试");
    }
}
