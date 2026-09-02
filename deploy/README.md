# 旅游系统 Docker Compose 部署说明

## 1. 部署目录

部署相关文件都在 `deploy/` 下：

```text
deploy/
  docker-compose.yml
  docker-compose.registry.yml
  deploy.sh
  update-images.sh
  publish-images.cmd
  publish-images.ps1
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
IMAGE_REGISTRY=ccr.ccs.tencentyun.com/lwystian/travel
MYSQL_ROOT_PASSWORD=change-this-root-password
MYSQL_PASSWORD=change-this-app-password
REDIS_PASSWORD=change-this-redis-password
APP_SUPER_ADMIN_INITIAL_PASSWORD=change-this-admin-password
USER_DEFAULT_PASSWORD=change-this-default-password
APP_SECURITY_ALLOWED_ORIGINS=https://your-domain.com,http://your-server-ip
APP_SECURITY_TOKEN_EXPIRE=2h
APP_UPLOAD_IMAGE_MAX_SIZE=2MB
KNIFE4J_BASIC_PASSWORD=change-this-doc-password
MINIAPP_API_BASE_URL=https://mini-api.your-domain.com/api
MINIAPP_API_ALLOWED_HOSTS=mini-api.your-domain.com
```

`MINIAPP_API_BASE_URL` 必须是 travel 后端容器能够访问的小程序 API 根地址。生产环境应使用 HTTPS 域名；如果两个后端在同一个 Compose 网络中，使用小程序后端的服务名，不要填写容器自身的 `localhost`。

部署后先在“网站设置 → 商品来源”测试连接，再切换到“小程序商品”。官网会从小程序 API 读取商品、套餐、班期和库存，订单与支付仍使用 travel 原有流程；配置保存在数据库中，环境变量只作为初始默认值。

`MINIAPP_API_ALLOWED_HOSTS` 是 travel 后端允许访问的小程序 API 域名白名单，多个域名用英文逗号分隔。生产环境建议配置；留空表示不限制，适合本地开发。

### 推荐：从镜像仓库启动

PC 前后端使用固定镜像标签，不需要维护具体版本号：

```text
ccr.ccs.tencentyun.com/lwystian/travel:pc-backend-latest
ccr.ccs.tencentyun.com/lwystian/travel:pc-frontend-latest
```

服务器首次登录腾讯云镜像仓库后，使用与小程序相同风格的部署命令启动，不在服务器执行 Maven 或 npm：

```bash
docker login ccr.ccs.tencentyun.com
./deploy.sh start
```

`docker-compose.yml` 定义本地构建镜像，`docker-compose.registry.yml` 只在服务器部署时把前后端映射到腾讯云固定标签。`deploy.sh` 会自动同时加载两个文件。

### 备用：从服务器源码构建

仅在镜像仓库暂时不可用、且服务器资源充足时使用：

```bash
docker compose build backend
docker compose build frontend
docker compose up -d
```

后端镜像使用阿里云 Maven 公共镜像和 BuildKit 依赖缓存，适合国内服务器构建。前后端顺序构建可以降低小内存服务器的瞬时资源占用。

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

### 5.1 Windows 本地发布镜像

本地安装并启动 Docker Desktop，在项目根目录执行与小程序一致的入口命令：

```powershell
.\publish
```

脚本会先串行构建所选本地镜像，再统一打标签并推送到腾讯云，不要求输入版本号。也可以只发布一个服务：

```powershell
.\publish backend
.\publish frontend
```

镜像推送中断后可直接复用本地构建结果重试：

```powershell
.\publish -PushOnly
```

### 5.2 服务器轻量更新

首次切换到镜像更新方式时，先拉取包含部署脚本的代码，并补充镜像仓库地址：

```bash
cd /root/travel
git pull --ff-only
cd deploy
grep -q '^IMAGE_REGISTRY=' .env || echo 'IMAGE_REGISTRY=ccr.ccs.tencentyun.com/lwystian/travel' >> .env
docker login ccr.ccs.tencentyun.com
./deploy.sh update
```

以后每次本地执行 `.\publish` 成功后，服务器只需要：

```bash
cd /root/travel/deploy
./deploy.sh update
```

更新脚本会自动完成：

1. 备份 MySQL 数据库并校验压缩包。
2. 给当前运行镜像保留 `pc-backend-previous`、`pc-frontend-previous` 标签。
3. 拉取两个 `latest` 镜像，不运行 Maven、npm 或 Docker build。
4. 先更新后端并等待健康，再更新前端。
5. 任一新容器健康检查失败时尝试恢复更新前镜像。

不要再在低内存生产服务器执行：

```bash
docker compose up -d --build backend frontend
```

MySQL、Redis、上传文件和备份目录都在 `TRAVEL_DATA_DIR` 下，不会因为重建容器丢失。

更新完成后可以确认实际运行镜像：

```bash
docker inspect travel-backend --format '{{.Image}}'
docker inspect travel-frontend --format '{{.Image}}'
./deploy.sh status
```

前端 `index.html` 已设置为禁止缓存，带哈希的 JS/CSS 文件仍使用长期缓存。这样每次容器切换后，浏览器会重新获取入口文件并加载新资源。

## 6. 备份

数据库备份：

```bash
docker compose exec mysql mysqldump -u root -p tourism_system > tourism_system.sql
```

完整迁移时，复制项目代码、`deploy/.env` 和 `TRAVEL_DATA_DIR` 即可。

## 7. 日志

容器日志：

```bash
./deploy.sh logs
./deploy.sh logs frontend
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
