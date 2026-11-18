package cn.edu.csu.mycoursestore.admin.service;

import cn.edu.csu.mycoursestore.admin.dto.AdminOrderDetail;
import cn.edu.csu.mycoursestore.admin.dto.AdminOrderFilter;
import cn.edu.csu.mycoursestore.admin.dto.AdminOrderLine;
import cn.edu.csu.mycoursestore.admin.dto.AdminOrderRow;
import cn.edu.csu.mycoursestore.admin.paging.PageQuery;
import cn.edu.csu.mycoursestore.admin.paging.PageResult;
import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.order.entity.OrderStatus;
import cn.edu.csu.mycoursestore.order.entity.SalesOrder;
import cn.edu.csu.mycoursestore.order.entity.SalesOrderItem;
import cn.edu.csu.mycoursestore.order.mapper.SalesOrderItemMapper;
import cn.edu.csu.mycoursestore.order.mapper.SalesOrderMapper;
import cn.edu.csu.mycoursestore.product.entity.Product;
import cn.edu.csu.mycoursestore.product.mapper.ProductMapper;
import cn.edu.csu.mycoursestore.user.entity.StoreUser;
import cn.edu.csu.mycoursestore.user.mapper.StoreUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminOrderService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SalesOrderMapper orderMapper;
    private final SalesOrderItemMapper orderItemMapper;
    private final StoreUserMapper userMapper;
    private final ProductMapper productMapper;

    public AdminOrderService(SalesOrderMapper orderMapper,
                             SalesOrderItemMapper orderItemMapper,
                             StoreUserMapper userMapper,
                             ProductMapper productMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
    }

    public PageResult<AdminOrderRow> list(AdminOrderFilter filter, PageQuery page) {
        LambdaQueryWrapper<SalesOrder> wrapper = buildWrapper(filter);
        long total = orderMapper.selectCount(wrapper);
        if (total == 0) {
            return PageResult.empty(page);
        }

        List<SalesOrder> orders = orderMapper.selectList(wrapper
                .orderByDesc(SalesOrder::getCreatedAt)
                .orderByDesc(SalesOrder::getId)
                .last("LIMIT " + page.size() + " OFFSET " + page.offset()));
        List<Long> orderIds = orders.stream().map(SalesOrder::getId).toList();
        Map<Long, StoreUser> users = userMap(orders.stream().map(SalesOrder::getUserId).distinct().toList());
        Map<Long, Integer> quantities = quantities(orderIds);

        List<AdminOrderRow> rows = orders.stream()
                .map(order -> {
                    StoreUser user = users.get(order.getUserId());
                    return new AdminOrderRow(
                            order.getId(),
                            order.getOrderNo(),
                            order.getUserId(),
                            user == null ? "未知" : user.getUsername(),
                            user == null ? "未知用户" : user.getDisplayName(),
                            order.getStatus(),
                            statusLabel(order.getStatus()),
                            order.getTotalAmount(),
                            quantities.getOrDefault(order.getId(), 0),
                            format(order.getCreatedAt()));
                })
                .toList();
        return new PageResult<>(rows, page.page(), page.size(), total);
    }

    public AdminOrderDetail detail(long id) {
        SalesOrder order = requireOrder(id);
        StoreUser user = userMapper.selectById(order.getUserId());
        List<AdminOrderLine> lines = orderItemMapper.selectList(new LambdaQueryWrapper<SalesOrderItem>()
                        .eq(SalesOrderItem::getOrderId, id)
                        .orderByAsc(SalesOrderItem::getId))
                .stream()
                .map(item -> new AdminOrderLine(
                        item.getProductId(),
                        item.getSkuSnapshot(),
                        item.getProductNameSnapshot(),
                        item.getCoverImageSnapshot(),
                        item.getUnitPrice(),
                        item.getQuantity() == null ? 0 : item.getQuantity(),
                        item.getLineAmount()))
                .toList();
        int totalQuantity = lines.stream().mapToInt(AdminOrderLine::quantity).sum();
        OrderStatus status = order.getStatus();

        return new AdminOrderDetail(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                user == null ? "未知" : user.getUsername(),
                user == null ? "未知用户" : user.getDisplayName(),
                status,
                statusLabel(status),
                order.getTotalAmount(),
                totalQuantity,
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                order.getPaymentMethod(),
                order.getPaymentNo(),
                format(order.getCreatedAt()),
                format(order.getPaidAt()),
                format(order.getCancelledAt()),
                format(order.getShippedAt()),
                format(order.getCompletedAt()),
                lines,
                status == OrderStatus.PENDING_PAYMENT,
                status == OrderStatus.PAID,
                status == OrderStatus.SHIPPED,
                status == OrderStatus.CANCELLED || status == OrderStatus.COMPLETED);
    }

    @Transactional
    public void ship(long id) {
        SalesOrder order = requireOrder(id);
        if (order.getStatus() != OrderStatus.PAID) {
            throw stateChanged();
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<SalesOrder>()
                .eq(SalesOrder::getId, id)
                .eq(SalesOrder::getStatus, OrderStatus.PAID)
                .set(SalesOrder::getStatus, OrderStatus.SHIPPED)
                .set(SalesOrder::getShippedAt, now)
                .set(SalesOrder::getUpdatedAt, now));
        if (rows == 0) {
            throw stateChanged();
        }
    }

    @Transactional
    public void complete(long id) {
        SalesOrder order = requireOrder(id);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw stateChanged();
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<SalesOrder>()
                .eq(SalesOrder::getId, id)
                .eq(SalesOrder::getStatus, OrderStatus.SHIPPED)
                .set(SalesOrder::getStatus, OrderStatus.COMPLETED)
                .set(SalesOrder::getCompletedAt, now)
                .set(SalesOrder::getUpdatedAt, now));
        if (rows == 0) {
            throw stateChanged();
        }
    }

    @Transactional
    public void cancel(long id) {
        SalesOrder order = requireOrder(id);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw stateChanged();
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<SalesOrder>()
                .eq(SalesOrder::getId, id)
                .eq(SalesOrder::getStatus, OrderStatus.PENDING_PAYMENT)
                .set(SalesOrder::getStatus, OrderStatus.CANCELLED)
                .set(SalesOrder::getCancelledAt, now)
                .set(SalesOrder::getUpdatedAt, now));
        if (rows == 0) {
            throw stateChanged();
        }

        List<SalesOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, id));
        for (SalesOrderItem item : items) {
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, item.getProductId())
                    .setSql("stock = stock + " + quantity
                            + ", version = version + 1, updated_at = LOCALTIMESTAMP"));
        }
    }

    @Transactional
    public void delete(long id) {
        SalesOrder order = requireOrder(id);
        if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "ORDER_DELETE_FORBIDDEN",
                    "只有已取消或已完成的订单可以删除");
        }

        orderItemMapper.delete(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, id));
        if (orderMapper.deleteById(id) == 0) {
            throw stateChanged();
        }
    }

    private LambdaQueryWrapper<SalesOrder> buildWrapper(AdminOrderFilter filter) {
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<>();
        if (filter.status() != null) {
            wrapper.eq(SalesOrder::getStatus, filter.status());
        }

        String keyword = trimToNull(filter.keyword());
        if (keyword != null) {
            List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<StoreUser>()
                            .select(StoreUser::getId)
                            .and(inner -> inner.like(StoreUser::getUsername, keyword)
                                    .or()
                                    .like(StoreUser::getDisplayName, keyword)))
                    .stream()
                    .map(StoreUser::getId)
                    .toList();
            wrapper.and(inner -> {
                inner.like(SalesOrder::getOrderNo, keyword)
                        .or()
                        .like(SalesOrder::getReceiverName, keyword)
                        .or()
                        .like(SalesOrder::getReceiverPhone, keyword);
                if (!userIds.isEmpty()) {
                    inner.or().in(SalesOrder::getUserId, userIds);
                }
            });
        }

        LocalDate from = filter.from();
        LocalDate to = filter.to();
        if (from != null && to != null && from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }
        if (from != null) {
            wrapper.ge(SalesOrder::getCreatedAt, from.atStartOfDay());
        }
        if (to != null) {
            wrapper.lt(SalesOrder::getCreatedAt, to.plusDays(1).atStartOfDay());
        }
        return wrapper;
    }

    private Map<Long, StoreUser> userMap(List<Long> userIds) {
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(StoreUser::getId, Function.identity()));
    }

    private Map<Long, Integer> quantities(List<Long> orderIds) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<SalesOrderItem>()
                        .select(SalesOrderItem::getOrderId, SalesOrderItem::getQuantity)
                        .in(SalesOrderItem::getOrderId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(SalesOrderItem::getOrderId,
                        Collectors.summingInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())));
    }

    private SalesOrder requireOrder(long id) {
        SalesOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在");
        }
        return order;
    }

    private static String statusLabel(OrderStatus status) {
        return status == null ? "未知" : status.getLabel();
    }

    private static String format(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMAT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static ApiException stateChanged() {
        return new ApiException(HttpStatus.CONFLICT, "ORDER_STATE_CHANGED", "订单状态已变化，请刷新后重试");
    }
}
