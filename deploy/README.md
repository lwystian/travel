# 旅游系统 Docker Compose 部署说明

## 1. 部署目录

部署相关文件都在 `deploy/` 下：

```text
deploy/
  docker-compose.yml
  .env.example
  mysql/conf.d/
  mysql/initdb/
```

Nginx 配置已经打进前端镜像，`deploy/` 下不再保留 Nginx 配置目录。

## 2. 最简部署

服务器只需要安装 Docker Engine 和 Docker Compose Plugin。

进入部署目录：

```bash
cd deploy
cp .env.example .env
```

编辑 `deploy/.env`，至少修改这些值：

```env
MYSQL_ROOT_PASSWORD=change-this-root-password
MYSQL_PASSWORD=change-this-app-password
REDIS_PASSWORD=change-this-redis-password
APP_SUPER_ADMIN_INITIAL_PASSWORD=change-this-admin-password
USER_DEFAULT_PASSWORD=change-this-default-password
APP_SECURITY_ALLOWED_ORIGINS=https://your-domain.com,http://your-server-ip
APP_SECURITY_TOKEN_EXPIRE=2h
APP_UPLOAD_IMAGE_MAX_SIZE=2MB
MINIAPP_API_BASE_URL=https://mini-api.your-domain.com/api
MINIAPP_BOOKING_URL_TEMPLATE="https://mini.your-domain.com/#/pages/tour/detail?id={tourId}&scheduleId={scheduleId}&packageId={packageId}"
MINIAPP_API_ALLOWED_HOSTS=mini-api.your-domain.com
```

`MINIAPP_API_BASE_URL` 必须是 travel 后端容器能够访问的小程序 API 根地址。生产环境应使用 HTTPS 域名；如果两个后端在同一个 Compose 网络中，使用小程序后端的服务名，不要填写容器自身的 `localhost`。

`MINIAPP_BOOKING_URL_TEMPLATE` 用于把官网选择交给小程序的统一订单流程，必须包含 `{tourId}`，还可以包含 `{scheduleId}` 和 `{packageId}`。部署后先在“网站设置 → 商品来源”测试连接，再切换到“小程序商品”。配置保存在数据库中，环境变量只作为初始默认值。

`MINIAPP_API_ALLOWED_HOSTS` 是 travel 后端允许访问的小程序 API 域名白名单，多个域名用英文逗号分隔。生产环境建议配置；留空表示不限制，适合本地开发。

启动：

```bash
docker compose up -d --build
```

Compose 会自动创建默认数据目录：

```text
deploy/travel-data/
  mysql/data    MySQL 数据
  redis/data    Redis 持久化数据
  app/files     上传的图片、视频等资源
  app/logs      后端文件日志
  app/backup    后台导出的日志备份
```

不需要手动执行 `mkdir` 或 `chown`。

## 3. SQL 放在哪里

首次部署新系统时，默认会执行：

```text
deploy/mysql/initdb/tourism_system_initial.sql
```

如果你有自己的完整 SQL 备份，并且是全新空库初始化，把 `.sql` 文件放到：

```text
deploy/mysql/initdb/
```

建议命名：

```text
01-full-backup.sql
```

MySQL 官方镜像只会在 `/var/lib/mysql` 为空时执行这个目录里的 SQL。也就是说，只在第一次初始化数据库时自动导入。

如果数据库已经启动过，需要手动恢复备份。命令在 `deploy/` 目录执行：

```bash
docker compose exec -T mysql mysql -u root -p tourism_system < backup.sql
```

如果要重新初始化空库，先停止服务并删除数据库数据目录：

```bash
docker compose down
rm -rf ./travel-data/mysql/data
docker compose up -d
```

生产环境执行删除前必须确认已经备份。

## 4. 使用固定数据目录

默认 `TRAVEL_DATA_DIR=./travel-data`，路径相对 `deploy/`，适合简单部署和项目整体迁移。

如果你希望数据固定放在服务器目录，例如 `/opt/travel-data`，只需要在 `deploy/.env` 改：

```env
TRAVEL_DATA_DIR=/opt/travel-data
```

Compose 会自动创建需要的子目录。若服务器权限策略较严格，才需要手动调整目录权限。

## 5. 更新应用

在 `deploy/` 目录执行：

```bash
docker compose up -d --build backend frontend
```

MySQL、Redis、上传文件和备份目录都在 `TRAVEL_DATA_DIR` 下，不会因为重建容器丢失。

## 6. 备份

数据库备份：

```bash
docker compose exec mysql mysqldump -u root -p tourism_system > tourism_system.sql
```

完整迁移时，复制项目代码、`deploy/.env` 和 `TRAVEL_DATA_DIR` 即可。

## 7. 日志

容器日志：

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

后端文件日志在：

```text
${TRAVEL_DATA_DIR}/app/logs
```

MySQL 和 Nginx 日志默认使用 Docker 日志系统，不再额外挂载到宿主机目录，减少部署目录和权限问题。

## 8. HTTPS

当前 Compose 只暴露 HTTP。生产环境建议在宿主机、宝塔面板、Nginx、Caddy 或云厂商负载均衡上配置 HTTPS。

启用 HTTPS 后，把 `deploy/.env` 中的来源限制改为真实域名：

```env
APP_SECURITY_ALLOWED_ORIGINS=https://travel.example.com
```
