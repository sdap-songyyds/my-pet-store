package cn.edu.csu.mycoursestore.cart.controller;

public final class CartMessages {

    private CartMessages() {
    }

    public static String of(String code) {
        return switch (code) {
            case "CART_FULL" -> "购物车最多容纳 20 种商品";
            case "OUT_OF_STOCK" -> "商品库存不足，请调整数量";
            case "PRODUCT_UNAVAILABLE" -> "商品不存在或已下架";
            case "QUANTITY_EXCEEDED" -> "同一商品最多购买 99 件";
            case "INVALID_QUANTITY" -> "购买数量不合法";
            case "CART_CONFLICT" -> "购物车已更新，请刷新后重试";
            case "CART_ITEM_NOT_FOUND" -> "购物车中没有该商品";
            default -> "操作失败，请稍后重试";
        };
    }
}