-- MyCourseStore 演示数据模板（PostgreSQL）
--
-- 用途：为本地商城前台提供示例分类、商品与相册数据，可按需修改后重复执行。
-- 用法：在任意 PostgreSQL 客户端中执行本文件，例如
--       psql -h localhost -U mypetstore_app -d mypetstore -f demo-data.sql
--
-- 特点：
--   * 不写死自增 id，全部通过 code / sku 关联，队友本地可直接运行。
--   * 可重复执行：分类按 code、商品按 sku 跳过已存在记录，相册按路径去重。
--   * 与 schema.sql 独立，不修改表结构。
--
-- 注意：category.parent_key 是数据库生成列（由 parent_id 自动派生），
--       脚本不写入该列，与 schema.sql 保持一致。
--
-- 使用前可把下面的示例行替换成自己的数据；图片使用站内相对路径
-- （静态资源目录 src/main/resources/static）。

BEGIN;

-- ===== 一级分类（parent_id 为空）=====
INSERT INTO category (code, parent_id, name, sort_order)
SELECT 'books', NULL, '图书', 10
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'books');

INSERT INTO category (code, parent_id, name, sort_order)
SELECT 'stationery', NULL, '文具', 20
WHERE NOT EXISTS (SELECT 1 FROM category WHERE code = 'stationery');

-- ===== 二级分类（parent_id 指向一级分类）=====
INSERT INTO category (code, parent_id, name, sort_order)
SELECT 'cs-books', parent.id, '计算机图书', 10
FROM category parent
WHERE parent.code = 'books'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'cs-books');

INSERT INTO category (code, parent_id, name, sort_order)
SELECT 'literature', parent.id, '文学图书', 20
FROM category parent
WHERE parent.code = 'books'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'literature');

INSERT INTO category (code, parent_id, name, sort_order)
SELECT 'pens', parent.id, '笔类', 10
FROM category parent
WHERE parent.code = 'stationery'
  AND NOT EXISTS (SELECT 1 FROM category WHERE code = 'pens');

-- ===== 在售商品（替换名称、简介、价格、库存等字段即可）=====
INSERT INTO product (sku, category_id, name, summary, description, cover_image, price, stock, status, sort_order)
SELECT 'DEMO-001', c.id, '示例商品 · 数据结构与算法',
       '演示用推荐商品，可替换为真实数据',
       '这里填写商品详情。当前为演示数据，请在 demo-data.sql 中替换。',
       'images/placeholder.svg', 89.00, 50, 'ON_SALE', 10
FROM category c
WHERE c.code = 'cs-books'
  AND NOT EXISTS (SELECT 1 FROM product WHERE sku = 'DEMO-001');

INSERT INTO product (sku, category_id, name, summary, description, cover_image, price, stock, status, sort_order)
SELECT 'DEMO-002', c.id, '示例商品 · 计算机网络原理',
       '演示用推荐商品，可替换为真实数据',
       '这里填写商品详情。当前为演示数据，请在 demo-data.sql 中替换。',
       'images/placeholder.svg', 69.00, 30, 'ON_SALE', 20
FROM category c
WHERE c.code = 'cs-books'
  AND NOT EXISTS (SELECT 1 FROM product WHERE sku = 'DEMO-002');

INSERT INTO product (sku, category_id, name, summary, description, cover_image, price, stock, status, sort_order)
SELECT 'DEMO-003', c.id, '示例商品 · 现代文学选读',
       '演示用零库存商品，用于展示缺货状态',
       '这里填写商品详情。当前为演示数据，请在 demo-data.sql 中替换。',
       'images/placeholder.svg', 45.50, 0, 'ON_SALE', 30
FROM category c
WHERE c.code = 'literature'
  AND NOT EXISTS (SELECT 1 FROM product WHERE sku = 'DEMO-003');

INSERT INTO product (sku, category_id, name, summary, description, cover_image, price, stock, status, sort_order)
SELECT 'DEMO-004', c.id, '示例商品 · 中性笔套装',
       '演示用推荐商品，可替换为真实数据',
       '这里填写商品详情。当前为演示数据，请在 demo-data.sql 中替换。',
       'images/placeholder.svg', 19.90, 100, 'ON_SALE', 40
FROM category c
WHERE c.code = 'pens'
  AND NOT EXISTS (SELECT 1 FROM product WHERE sku = 'DEMO-004');

-- ===== 商品相册（每个商品一条，可继续追加不同图片）=====
INSERT INTO product_image (product_id, image_path, alt_text, sort_order)
SELECT p.id, 'images/placeholder.svg', p.name, 0
FROM product p
WHERE p.sku IN ('DEMO-001', 'DEMO-002', 'DEMO-003', 'DEMO-004')
  AND NOT EXISTS (
      SELECT 1 FROM product_image pi
      WHERE pi.product_id = p.id AND pi.image_path = 'images/placeholder.svg'
  );

COMMIT;

-- ===== 管理员账号 =====
-- 用户名：admin
-- 初始密码：Admin@123456
-- password_hash 由 Spring Security BCryptPasswordEncoder 生成；
-- 重复执行不会覆盖已存在的 admin，也不会把同名普通账号提升为管理员。
BEGIN;

INSERT INTO store_user (username, password_hash, display_name, role, status)
SELECT 'admin',
       '$2a$10$aPZI5WVwYcciEjby5kBtuuZlBqnWkr3ITnL.sAblVWiAoZm2v9MV2',
       '管理员',
       'ADMIN',
       'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM store_user WHERE username = 'admin');

COMMIT;
