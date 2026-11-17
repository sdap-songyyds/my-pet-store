-- MyPetStore 实验一：创建“项目角色 + 项目数据库”（PostgreSQL 18）
-- 以管理员角色 deepseek 执行，连到 postgres 库：
--     psql -U deepseek -d postgres -f db/00-create-role-and-database.sql
-- 本文件只创建角色和库，不建表；表结构见 01-schema-postgresql.sql。
-- 重复执行会报 “already exists”，属正常，不是数据损坏。

-- 1. 项目专用登录角色。
--    库里所有对象的属主都是它，因此它在本库内可以自由建表，
--    但 NOSUPERUSER/NOCREATEDB 保证它管不到别的库和服务器。
CREATE ROLE mypetstore_app
    LOGIN
    PASSWORD 'password'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOREPLICATION;

-- 2. 项目数据库，属主直接给项目角色，省去逐条 GRANT。
--    TEMPLATE template0 + UTF8：与 MySQL 版的 utf8mb4 对应，
--    保证中文与 emoji 都能存；排序规则用库默认即可。
CREATE DATABASE mypetstore
    OWNER mypetstore_app
    ENCODING 'UTF8'
    TEMPLATE template0;

-- 3. 会话时区对齐 MySQL 版的 SET SESSION time_zone = '+08:00'，
--    这样 TIMESTAMP 类型的 created_at/paid_at 等按东八区落库。
ALTER DATABASE mypetstore SET TIME ZONE 'Asia/Shanghai';

-- 4. 确认结果：
--    列出角色 -> \du mypetstore_app
--    列出库   -> \l mypetstore
