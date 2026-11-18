package cn.edu.csu.mycoursestore.cart.service;

import cn.edu.csu.mycoursestore.cart.dto.CartLine;
import cn.edu.csu.mycoursestore.cart.dto.CartView;
import cn.edu.csu.mycoursestore.cart.entity.CartItem;
import cn.edu.csu.mycoursestore.cart.mapper.CartItemMapper;
import cn.edu.csu.mycoursestore.common.ApiException;
import cn.edu.csu.mycoursestore.product.entity.Product;
import cn.edu.csu.mycoursestore.product.entity.ProductStatus;
import cn.edu.csu.mycoursestore.product.mapper.ProductMapper;
import cn.edu.csu.mycoursestore.user.entity.StoreUser;
import cn.edu.csu.mycoursestore.user.mapper.StoreUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartService {

    public static final int MAX_KINDS = 20;
    public static final int MAX_QUANTITY = 99;

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final StoreUserMapper storeUserMapper;

    public CartService(CartItemMapper cartItemMapper, ProductMapper productMapper, StoreUserMapper storeUserMapper) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.storeUserMapper = storeUserMapper;
    }

    public CartView getCart(long userId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByAsc(CartItem::getCreatedAt)
                .orderByAsc(CartItem::getId));
        if (items.isEmpty()) {
            return new CartView(List.of(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0, 0, false);
        }

        List<Long> productIds = items.stream().map(CartItem::getProductId).toList();
        Map<Long, Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .in(Product::getId, productIds))
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<CartLine> lines = new ArrayList<>(items.size());
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (CartItem item : items) {
            Product product = products.get(item.getProductId());
            boolean onSale = product != null && product.getStatus() == ProductStatus.ON_SALE;
            int stock = stockOf(product);
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            BigDecimal price = product == null ? BigDecimal.ZERO : product.getPrice();
            BigDecimal lineAmount = price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

            if (onSale) {
                totalAmount = totalAmount.add(lineAmount);
            }
            totalQuantity += quantity;
            lines.add(new CartLine(
                    item.getProductId(),
                    product == null ? "商品已删除" : product.getName(),
                    product == null ? "images/placeholder.svg" : product.getCoverImage(),
                    price,
                    stock,
                    quantity,
                    Math.min(stock, MAX_QUANTITY),
                    lineAmount,
                    onSale,
                    onSale && stock >= quantity));
        }
        boolean checkoutReady = lines.stream().allMatch(line -> line.onSale() && line.stockEnough());
        return new CartView(lines, totalAmount.setScale(2, RoundingMode.HALF_UP), totalQuantity,
                lines.size(), checkoutReady);
    }

    public int cartQuantity(long userId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .select(CartItem::getQuantity)
                .eq(CartItem::getUserId, userId));
        return items.stream().mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity()).sum();
    }

    @Transactional
    public void add(long userId, long productId, int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw invalidQuantity();
        }
        lockUser(userId);
        Product product = requireSaleableProduct(productId);
        int stock = stockOf(product);

        CartItem existing = findItem(userId, productId);
        if (existing == null) {
            long kinds = cartItemMapper.selectCount(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, userId));
            if (kinds >= MAX_KINDS) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "CART_FULL",
                        "购物车最多容纳 " + MAX_KINDS + " 种商品");
            }
            checkQuantity(quantity, stock);
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(productId);
            item.setQuantity(quantity);
            item.setVersion(0L);
            try {
                cartItemMapper.insert(item);
            } catch (DuplicateKeyException e) {
                throw new ApiException(HttpStatus.CONFLICT, "CART_CONFLICT", "购物车已更新，请重试");
            }
        } else {
            int newQuantity = existing.getQuantity() + quantity;
            checkQuantity(newQuantity, stock);
            conditionalUpdate(existing, newQuantity);
        }
    }

    @Transactional
    public void updateQuantity(long userId, long productId, int quantity) {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw invalidQuantity();
        }
        lockUser(userId);
        Product product = requireSaleableProduct(productId);
        CartItem item = findItem(userId, productId);
        if (item == null) {
            throw itemNotFound();
        }
        checkQuantity(quantity, stockOf(product));
        conditionalUpdate(item, quantity);
    }

    @Transactional
    public void remove(long userId, long productId) {
        int rows = cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, productId));
        if (rows == 0) {
            throw itemNotFound();
        }
    }

    private void lockUser(long userId) {
        StoreUser user = storeUserMapper.selectOne(new LambdaQueryWrapper<StoreUser>()
                .eq(StoreUser::getId, userId)
                .last("for update"));
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "登录已失效，请重新登录");
        }
    }

    private Product requireSaleableProduct(long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStatus() != ProductStatus.ON_SALE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PRODUCT_UNAVAILABLE", "商品不存在或已下架");
        }
        return product;
    }

    private CartItem findItem(long userId, long productId) {
        return cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, productId));
    }

    private void checkQuantity(int quantity, int stock) {
        if (quantity > MAX_QUANTITY) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "QUANTITY_EXCEEDED",
                    "同一商品最多购买 " + MAX_QUANTITY + " 件");
        }
        if (quantity > stock) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OUT_OF_STOCK", "库存不足，当前仅剩 " + stock + " 件");
        }
    }

    private void conditionalUpdate(CartItem item, int newQuantity) {
        int rows = cartItemMapper.update(null, new LambdaUpdateWrapper<CartItem>()
                .eq(CartItem::getId, item.getId())
                .eq(CartItem::getVersion, item.getVersion())
                .set(CartItem::getQuantity, newQuantity)
                .set(CartItem::getVersion, item.getVersion() + 1)
                .set(CartItem::getUpdatedAt, LocalDateTime.now()));
        if (rows == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "CART_CONFLICT", "购物车已更新，请刷新后重试");
        }
    }

    private static int stockOf(Product product) {
        return product == null || product.getStock() == null ? 0 : product.getStock();
    }

    private static ApiException invalidQuantity() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY", "购买数量不合法");
    }

    private static ApiException itemNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", "购物车中没有该商品");
    }
}