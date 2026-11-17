-- 结构验收脚本：在事务中造数据并故意触发各条约束，最后 ROLLBACK，库里不留数据。
BEGIN;

-- 1) 正常插入 + 生成列：一级分类 parent_id 为空时 parent_key 应为 0
INSERT INTO store_user (username, password_hash, display_name, role)
VALUES ('alice', '$2a$10$abcdefghijklmnopqrstuv', '爱丽丝', 'CUSTOMER');

INSERT INTO category (code, parent_id, name)
VALUES ('CAT_ROOT', NULL, '宠物');

SELECT '1) 生成列 parent_key' AS check_item,
       CASE WHEN parent_key = 0 THEN 'PASS' ELSE 'FAIL' END AS result
FROM category WHERE code = 'CAT_ROOT';

-- 2) updated_at 触发器：手工写旧值，UPDATE 后应被改成当前时间
UPDATE store_user SET display_name = '爱丽丝A', updated_at = '1999-01-01 00:00:00'
WHERE username = 'alice';

SELECT '2) updated_at 触发器' AS check_item,
       CASE WHEN updated_at > '2020-01-01' THEN 'PASS' ELSE 'FAIL' END AS result
FROM store_user WHERE username = 'alice';

-- 3) 角色枚举：小写 admin 必须被拒（对应 ascii_bin 区分大小写）
DO $$
BEGIN
    BEGIN
        INSERT INTO store_user (username, password_hash, display_name, role)
        VALUES ('bob', 'x', '鲍勃', 'admin');
        RAISE NOTICE '3) 角色 CHECK: FAIL（错误地接受了 admin）';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE '3) 角色 CHECK: PASS（拒绝 admin）';
    END;
END $$;

-- 4) 外键：二级分类指向不存在的父级
DO $$
BEGIN
    BEGIN
        INSERT INTO category (code, parent_id, name) VALUES ('CAT_ORPHAN', 99999, '孤儿');
        RAISE NOTICE '4) 分类外键: FAIL（接受了不存在的父级）';
    EXCEPTION WHEN foreign_key_violation THEN
        RAISE NOTICE '4) 分类外键: PASS（拒绝不存在的父级）';
    END;
END $$;

-- 5) 订单状态与支付字段的一致性
DO $$
BEGIN
    BEGIN
        INSERT INTO sales_order (order_no, user_id, status, total_amount,
                                 receiver_name, receiver_phone, receiver_address)
        VALUES ('ORD-BAD', 1, 'PAID', 10.00, '张三', '13800000000', '长沙市');
        RAISE NOTICE '5) 支付信息 CHECK: FAIL（PAID 缺少 payment_no）';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE '5) 支付信息 CHECK: PASS（拒绝缺少凭证的 PAID）';
    END;
END $$;

-- 6) 明细金额必须等于单价×数量
INSERT INTO product (sku, category_id, name, price, stock, status)
VALUES ('SKU-T1', (SELECT id FROM category WHERE code = 'CAT_ROOT'), '测试猫粮', 9.90, 10, 'ON_SALE');

INSERT INTO sales_order (order_no, user_id, status, total_amount,
                         receiver_name, receiver_phone, receiver_address)
VALUES ('ORD-OK', 1, 'PENDING_PAYMENT', 19.80, '张三', '13800000000', '长沙市');

DO $$
BEGIN
    BEGIN
        INSERT INTO sales_order_item (order_id, product_id, sku_snapshot,
               product_name_snapshot, cover_image_snapshot, unit_price, quantity, line_amount)
        VALUES ((SELECT id FROM sales_order WHERE order_no='ORD-OK'),
                (SELECT id FROM product WHERE sku='SKU-T1'),
                'SKU-T1', '测试猫粮', 'images/logo.png', 9.90, 2, 999.00);
        RAISE NOTICE '6) 明细金额 CHECK: FAIL（接受了错误合计）';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE '6) 明细金额 CHECK: PASS（拒绝错误合计）';
    END;
END $$;

-- 7) 库存不能为负
DO $$
BEGIN
    BEGIN
        INSERT INTO product (sku, category_id, name, price, stock)
        VALUES ('SKU-NEG', (SELECT id FROM category WHERE code='CAT_ROOT'), '负库存', 1.00, -1);
        RAISE NOTICE '7) 库存 CHECK: FAIL（接受了负库存）';
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE '7) 库存 CHECK: PASS（拒绝负库存）';
    END;
END $$;

ROLLBACK;
