package cn.edu.csu.mycoursestore.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartView(
        List<CartLine> lines,
        BigDecimal totalAmount,
        int totalQuantity,
        int kinds
) {
}