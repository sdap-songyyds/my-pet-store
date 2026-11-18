# MyCourseStore

基于 Spring Boot、MyBatis-Plus、Thymeleaf 与 PostgreSQL 的前后端不分离商城。

## 本地运行

1. 初始化角色与数据库（只需一次）：

   ```bash
   psql -U postgres -d postgres -f db/00-create-role-and-database.sql
   psql -h localhost -U mypetstore_app -d mypetstore -f db/01-schema-postgresql.sql
   ```

2. 导入演示数据（可重复执行，含分类、示例商品和管理员账号）：

   ```bash
   psql -h localhost -U mypetstore_app -d mypetstore -f demo-data.sql
   ```

3. 按需复制 `.env.example` 为 `.env` 并填写数据库连接信息。

4. 启动：

   ```bash
   ./mvnw spring-boot:run
   ```

   默认访问 `http://localhost:8080`。

## 管理员账号

`demo-data.sql` 末尾会幂等插入一个管理员账号：

- 用户名：`admin`
- 初始密码：`Admin@123456`

该密码对应的 BCrypt 哈希已写入 `demo-data.sql`。重复执行脚本不会重置已存在账号的密码。
