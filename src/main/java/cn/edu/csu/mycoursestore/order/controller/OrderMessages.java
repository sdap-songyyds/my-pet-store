package cn.edu.csu.mycoursestore.order.controller;

public final class OrderMessages {

    private OrderMessages() {
    }

    public static String of(String code) {
        return switch (code) {
            case "CART_EMPTY" -> "购物车为空，请先挑选商品";
            case "OUT_OF_STOCK" -> "部分商品库存不足，请返回购物车调整";
            case "PRODUCT_UNAVAILABLE" -> "部分商品不存在或已下架";
            case "ORDER_CONFLICT" -> "库存已变化，请返回购物车重试";
            case "ORDER_STATE_CHANGED" -> "订单状态已变化，请刷新后重试";
            case "ORDER_NOT_FOUND" -> "订单不存在";
            default -> "操作失败，请稍后重试";
        };
    }
}