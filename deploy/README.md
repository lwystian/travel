# 旅游系统 Docker Compose 生产部署说明

这套部署把所有需要持久化的数据都放在同一个宿主机目录下：

```text
${TRAVEL_DATA_DIR}/
  mysql/data      # MySQL 数据
  mysql/logs      # MySQL 日志
  redis/data      # Redis AOF/RDB 数据
  app/files       # 后端上传的图片、视频等资源
  app/logs        # Spring Boot 日志
  app/backup      # 系统备份目录
  nginx/logs      # Nginx 访问日志
```

后续迁移时，复制项目代码、`.env` 和 `${TRAVEL_DATA_DIR}` 即可。

## 1. 服务器准备

在 Linux 服务器安装 Docker Engine 和 Docker Compose Plugin，然后创建统一数据目录：

```bash
sudo mkdir -p /opt/travel-data/{mysql/data,mysql/logs,redis/data,app/files,app/logs,app/backup,nginx/logs}
sudo chown -R "$USER":"$USER" /opt/travel-data
```

如果你的服务器使用独立磁盘，建议把磁盘挂载到 `/opt/travel-data`，这样迁移和扩容会很舒服。

## 2. 环境变量配置

在项目根目录复制模板：

```bash
cp .env.example .env
```

然后编辑 `.env`：

- 修改所有 `change-this-*` 密码。
- `TRAVEL_DATA_DIR` 保持 `/opt/travel-data`，或改成你的统一数据目录。
- `APP_SECURITY_ALLOWED_ORIGINS` 改成真实域名/IP，例如：`https://travel.example.com,http://1.2.3.4`。
- `APP_SECURITY_PASSWORD_ENCRYPTION_ENABLED=true` 是登录/注册密码 RSA 加密开关，生产环境建议保持开启。
- `APP_SUPER_ADMIN_*` 是首次启动时自动初始化超级管理员的配置。

生产环境不要把 `.env` 提交到 Git。

## 3. 数据库初始化

如果你有完整 SQL 备份，把 `.sql` 文件放到：

```text
deploy/mysql/initdb/
```

注意：这个目录里的 SQL 只会在 `${TRAVEL_DATA_DIR}/mysql/data` 为空、MySQL 第一次初始化时执行。

如果数据库已经初始化过，需要手动导入：

```bash
docker compose exec -T mysql mysql -u root -p tourism_system < backup.sql
```

## 4. 构建并启动

```bash
docker compose --env-file .env up -d --build
```

查看状态：

```bash
docker compose ps
docker compose logs -f backend
```

访问：

```text
http://你的服务器IP/
```

接口文档：

```text
http://你的服务器IP/doc.html
```

## 5. 日常更新

上传或拉取新代码后，只需要重新构建应用镜像：

```bash
docker compose --env-file .env build backend frontend
docker compose --env-file .env up -d backend frontend
```

MySQL、Redis、上传资源、日志、备份都在 `${TRAVEL_DATA_DIR}`，不会跟随容器重建丢失。

## 6. 备份与迁移

冷迁移：

```bash
docker compose down
sudo tar -czf travel-data.tar.gz -C /opt travel-data
```

新服务器恢复到同一路径，再带着同一份 `.env` 启动即可。

在线数据库备份：

```bash
docker compose exec mysql mysqldump -u root -p tourism_system > tourism_system.sql
```

## 7. HTTPS 建议

当前 compose 只暴露 HTTP。生产环境建议在宿主机或云负载均衡上做 HTTPS，例如 Caddy、Nginx、宝塔、1Panel、云厂商负载均衡均可。

启用 HTTPS 后，把 `.env` 里的来源限制改成你的 HTTPS 域名：

```env
APP_SECURITY_ALLOWED_ORIGINS=https://travel.example.com
APP_SECURITY_PASSWORD_ENCRYPTION_ENABLED=true
```
