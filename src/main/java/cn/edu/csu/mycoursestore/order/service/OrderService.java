package cn.edu.csu.mycoursestore.order.service;

import cn.edu.csu.mycoursestore.cart.entity.CartItem;
import cn.edu.csu.mycoursestore.cart.mapper.CartItemMapper;
import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.order.dto.CheckoutRequest;
import cn.edu.csu.mycoursestore.order.dto.OrderDetail;
import cn.edu.csu.mycoursestore.order.dto.OrderLine;
import cn.edu.csu.mycoursestore.order.dto.OrderSummary;
import cn.edu.csu.mycoursestore.order.entity.OrderStatus;
import cn.edu.csu.mycoursestore.order.entity.SalesOrder;
import cn.edu.csu.mycoursestore.order.entity.SalesOrderItem;
import cn.edu.csu.mycoursestore.order.mapper.SalesOrderItemMapper;
import cn.edu.csu.mycoursestore.order.mapper.SalesOrderMapper;
import cn.edu.csu.mycoursestore.product.entity.Product;
import cn.edu.csu.mycoursestore.product.entity.ProductStatus;
import cn.edu.csu.mycoursestore.product.mapper.ProductMapper;
import cn.edu.csu.mycoursestore.user.entity.StoreUser;
import cn.edu.csu.mycoursestore.user.mapper.StoreUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String PAYMENT_METHOD = "MOCK";

    private final SalesOrderMapper orderMapper;
    private final SalesOrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final StoreUserMapper storeUserMapper;

    public OrderService(SalesOrderMapper orderMapper,
                        SalesOrderItemMapper orderItemMapper,
                        CartItemMapper cartItemMapper,
                        ProductMapper productMapper,
                        StoreUserMapper storeUserMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.storeUserMapper = storeUserMapper;
    }

    public List<OrderSummary> listOrders(long userId) {
        List<SalesOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getUserId, userId)
                .orderByDesc(SalesOrder::getCreatedAt)
                .orderByDesc(SalesOrder::getId));
        if (orders.isEmpty()) {
            return List.of();
        }

        List<Long> orderIds = orders.stream().map(SalesOrder::getId).toList();
        Map<Long, Integer> quantityByOrder = orderItemMapper.selectList(
                        new LambdaQueryWrapper<SalesOrderItem>().in(SalesOrderItem::getOrderId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(SalesOrderItem::getOrderId,
                        Collectors.summingInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())));

        return orders.stream()
                .map(order -> new OrderSummary(
                        order.getId(),
                        order.getOrderNo(),
                        order.getStatus(),
                        order.getStatus().getLabel(),
                        order.getTotalAmount(),
                        quantityByOrder.getOrDefault(order.getId(), 0),
                        format(order.getCreatedAt())))
                .toList();
    }

    public OrderDetail getOrder(long userId, long orderId) {
        SalesOrder order = requireOwnedOrder(userId, orderId);
        List<OrderLine> lines = orderItemMapper.selectList(new LambdaQueryWrapper<SalesOrderItem>()
                        .eq(SalesOrderItem::getOrderId, orderId)
                        .orderByAsc(SalesOrderItem::getId))
                .stream()
                .map(item -> new OrderLine(item.getProductId(), item.getSkuSnapshot(),
                        item.getProductNameSnapshot(), item.getCoverImageSnapshot(), item.getUnitPrice(),
                        item.getQuantity() == null ? 0 : item.getQuantity(), item.getLineAmount()))
                .toList();
        boolean pending = order.getStatus() == OrderStatus.PENDING_PAYMENT;
        return new OrderDetail(order.getId(), order.getOrderNo(), order.getStatus(),
                order.getStatus().getLabel(), order.getTotalAmount(),
                order.getReceiverName(), order.getReceiverPhone(), order.getReceiverAddress(),
                order.getPaymentMethod(), order.getPaymentNo(),
                format(order.getCreatedAt()), format(order.getPaidAt()), format(order.getCancelledAt()),
                format(order.getShippedAt()), format(order.getCompletedAt()),
                lines, pending, pending);
    }

    @Transactional
    public long createFromCart(long userId, CheckoutRequest request) {
        lockUser(userId);

        List<CartItem> cartItems = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByAsc(CartItem::getCreatedAt)
                .orderByAsc(CartItem::getId));
        if (cartItems.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CART_EMPTY", "购物车为空");
        }

        List<Long> productIds = cartItems.stream().map(CartItem::getProductId).toList();
        Map<Long, Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .in(Product::getId, productIds))
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<PreparedLine> preparedLines = new ArrayList<>(cartItems.size());
        for (CartItem cartItem : cartItems) {
            Product product = products.get(cartItem.getProductId());
            if (product == null || product.getStatus() != ProductStatus.ON_SALE) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "PRODUCT_UNAVAILABLE",
                        "商品不存在或已下架：" + (product == null ? cartItem.getProductId() : product.getName()));
            }
            int quantity = cartItem.getQuantity() == null ? 0 : cartItem.getQuantity();
            int stock = product.getStock() == null ? 0 : product.getStock();
            if (quantity > stock) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "OUT_OF_STOCK", "商品库存不足：" + product.getName());
            }
            BigDecimal lineAmount = product.getPrice()
                    .multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(lineAmount);
            preparedLines.add(new PreparedLine(product, quantity, lineAmount));
        }

        SalesOrder order = new SalesOrder();
        order.setOrderNo(newSerial("ORD"));
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        order.setReceiverName(request.receiverName().trim());
        order.setReceiverPhone(request.receiverPhone().trim());
        order.setReceiverAddress(request.receiverAddress().trim());
        orderMapper.insert(order);

        for (PreparedLine line : preparedLines) {
            SalesOrderItem item = new SalesOrderItem();
            item.setOrderId(order.getId());
            item.setProductId(line.product.getId());
            item.setSkuSnapshot(line.product.getSku());
            item.setProductNameSnapshot(line.product.getName());
            item.setCoverImageSnapshot(line.product.getCoverImage());
            item.setUnitPrice(line.product.getPrice());
            item.setQuantity(line.quantity);
            item.setLineAmount(line.lineAmount);
            orderItemMapper.insert(item);

            int rows = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, line.product.getId())
                    .eq(Product::getVersion, line.product.getVersion())
                    .ge(Product::getStock, line.quantity)
                    .setSql("stock = stock - " + line.quantity
                            + ", version = version + 1, updated_at = LOCALTIMESTAMP"));
            if (rows == 0) {
                throw new ApiException(HttpStatus.CONFLICT, "ORDER_CONFLICT", "库存已变化，请返回购物车重试");
            }
        }

        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
        return order.getId();
    }

    @Transactional
    public void pay(long userId, long orderId) {
        SalesOrder order = requireOwnedOrder(userId, orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw stateChanged();
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<SalesOrder>()
                .eq(SalesOrder::getId, orderId)
                .eq(SalesOrder::getUserId, userId)
                .eq(SalesOrder::getStatus, OrderStatus.PENDING_PAYMENT)
                .set(SalesOrder::getStatus, OrderStatus.PAID)
                .set(SalesOrder::getPaymentMethod, PAYMENT_METHOD)
                .set(SalesOrder::getPaymentNo, newSerial("PAY"))
                .set(SalesOrder::getPaidAt, now)
                .set(SalesOrder::getUpdatedAt, now));
        if (rows == 0) {
            throw stateChanged();
        }
    }

    @Transactional
    public void cancel(long userId, long orderId) {
        SalesOrder order = requireOwnedOrder(userId, orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw stateChanged();
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<SalesOrder>()
                .eq(SalesOrder::getId, orderId)
                .eq(SalesOrder::getUserId, userId)
                .eq(SalesOrder::getStatus, OrderStatus.PENDING_PAYMENT)
                .set(SalesOrder::getStatus, OrderStatus.CANCELLED)
                .set(SalesOrder::getCancelledAt, now)
                .set(SalesOrder::getUpdatedAt, now));
        if (rows == 0) {
            throw stateChanged();
        }

        List<SalesOrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, orderId));
        for (SalesOrderItem item : items) {
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, item.getProductId())
                    .setSql("stock = stock + " + quantity
                            + ", version = version + 1, updated_at = LOCALTIMESTAMP"));
        }
    }

    private SalesOrder requireOwnedOrder(long userId, long orderId) {
        SalesOrder order = orderMapper.selectOne(new LambdaQueryWrapper<SalesOrder>()
                .eq(SalesOrder::getId, orderId)
                .eq(SalesOrder::getUserId, userId));
        if (order == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在");
        }
        return order;
    }

    private void lockUser(long userId) {
        StoreUser user = storeUserMapper.selectOne(new LambdaQueryWrapper<StoreUser>()
                .eq(StoreUser::getId, userId)
                .last("for update"));
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "登录已失效，请重新登录");
        }
    }

    private static ApiException stateChanged() {
        return new ApiException(HttpStatus.CONFLICT, "ORDER_STATE_CHANGED", "订单状态已变化，请刷新后重试");
    }

    private static String newSerial(String prefix) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return prefix + timestamp + random;
    }

    private static String format(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMAT);
    }

    private record PreparedLine(Product product, int quantity, BigDecimal lineAmount) {
    }
}