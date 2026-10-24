# 部署说明

## 一、环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17+ | 后端运行 |
| Maven | 3.8+ | 后端构建 |
| MySQL | 8.0+ | 建议独立数据库账号 |
| Node.js | 18+ | 前端构建 |
| Nginx | 1.20+ | 可选，用于 HTTPS/域名反向代理 |
| Tomcat | 10.1.x | 可选，仅 WAR 部署时需要（必须是 Jakarta EE 10 版本） |

服务器建议至少 2 核 4 GB，附件磁盘按业务量预留并定期扩容。生产环境需保证数据库与附件目录在同一备份策略内。

## 二、初始化数据库

```bash
mysql -uroot -p < sql/01_schema.sql
mysql -uroot -p < sql/02_init_data.sql
```

创建独立业务账号（示例）：

```sql
CREATE USER 'archive_app'@'127.0.0.1' IDENTIFIED BY '请替换为强密码';
GRANT SELECT, INSERT, UPDATE, DELETE ON archive_contract.* TO 'archive_app'@'127.0.0.1';
FLUSH PRIVILEGES;
```

> 业务账号不需要 `DROP`、`ALTER`、`TRIGGER` 权限。触发器由建表脚本以管理员账号创建。

验证：

```sql
USE archive_contract;
SHOW TABLES;                 -- 应返回 17 张表
SELECT COUNT(*) FROM sys_permission;
SELECT trigger_name FROM information_schema.triggers
WHERE trigger_schema = 'archive_contract';   -- 应返回 6 个触发器
```

## 三、构建与启动（前后端一体化）

### 1. 构建

Windows 一键构建（前端 + 后端）：

```powershell
cd E:\dangan
.\build-all.ps1
```

脚本依次执行前端构建、复制 `frontend/dist` 到后端 `static`、Maven 打包。也可以手动执行：

```bash
cd frontend
npm ci
npm run build

cd ..
mvn -f backend/pom.xml clean package -DskipTests
```

Maven 的 `prepare-package` 阶段会自动把 `frontend/dist` 复制进 jar，最终产物：

`backend/target/archive-contract-system-1.0.0.jar`

该 jar 已内嵌前端页面，启动后无需再单独部署 Nginx 静态目录或运行 Vite。

### 2. 环境变量

| 变量 | 示例 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | 服务端口（仅 jar；WAR 由 Tomcat 决定） |
| `DB_HOST` | `127.0.0.1` | MySQL 地址 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `archive_contract` | 数据库 |
| `DB_USERNAME` | `archive_app` | 数据库账号 |
| `DB_PASSWORD` | `******` | 数据库密码 |
| `JWT_SECRET` | 随机 48 字节以上 | 必换，泄露需立即轮换 |
| `JWT_EXPIRE_HOURS` | `8` | Token 有效期 |
| `FILE_STORAGE_PATH` | `/opt/archive/data/uploads` | 建议绝对路径 |
| `CORS_ORIGINS` | `https://archive.example.com` | 多个用英文逗号分隔 |

生成 JWT 密钥：

```bash
openssl rand -base64 48
```

### 3. Linux 前台启动

```bash
export DB_HOST=127.0.0.1
export DB_NAME=archive_contract
export DB_USERNAME=archive_app
export DB_PASSWORD='******'
export JWT_SECRET='******'
export FILE_STORAGE_PATH=/opt/archive/data/uploads

mkdir -p /opt/archive/data/uploads /opt/archive/logs
cd /opt/archive
java -jar backend/archive-contract-system-1.0.0.jar
```

### 4. Windows PowerShell 启动

```powershell
$env:DB_HOST = '127.0.0.1'
$env:DB_USERNAME = 'archive_app'
$env:DB_PASSWORD = '******'
$env:JWT_SECRET = '******'
$env:FILE_STORAGE_PATH = 'D:\archive-data\uploads'
java -jar backend\target\archive-contract-system-1.0.0.jar
```

### 5. systemd 托管（推荐）

`/etc/systemd/system/archive-system.service`：

```ini
[Unit]
Description=Archive Contract System
After=network.target mysql.service

[Service]
Type=simple
User=archive
WorkingDirectory=/opt/archive
EnvironmentFile=/opt/archive/archive.env
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/archive/backend/archive-contract-system-1.0.0.jar
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

`/opt/archive/archive.env`：

```ini
DB_HOST=127.0.0.1
DB_PORT=3306
DB_NAME=archive_contract
DB_USERNAME=archive_app
DB_PASSWORD=******
JWT_SECRET=******
FILE_STORAGE_PATH=/opt/archive/data/uploads
SERVER_PORT=8080
```

```bash
chmod 600 /opt/archive/archive.env
systemctl daemon-reload
systemctl enable --now archive-system
systemctl status archive-system
```

Windows 可使用 NSSM、WinSW 或任务计划程序将 `java -jar` 注册为服务，工作目录必须设置为 jar 所在目录或配置绝对附件路径。

### 6. 健康检查

```bash
# 前端页面
curl -I http://127.0.0.1:8080/

# 登录接口
curl -i http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

根路径返回 `200 text/html`，登录接口返回 `code=200` 且包含 `token` 即启动成功。直接访问 `http://127.0.0.1:8080/` 即可打开系统。

## 四、部署到外部 Tomcat（WAR）

### 1. 版本要求

- JDK 17
- Apache Tomcat **10.1.x**（Jakarta EE 10）。Spring Boot 3 使用 `jakarta.*` 命名空间，Tomcat 9 及 8.5 **不能**运行本 war。
- MySQL 8.0 与前面章节相同。

### 2. 构建

```powershell
cd E:\dangan
# 默认部署到 /archive 上下文
.\build-war.ps1 -ContextPath archive
# 如需部署为根路径（war 名 ROOT.war）
.\build-war.ps1 -ContextPath ROOT
```

脚本会：

1. 以 `VITE_BASE=/archive/`（或 `/`）构建前端；
2. 同步前端到 `backend/src/main/resources/static`；
3. 执行 `mvn clean package -Pwar -DskipTests`；
4. 输出 `backend/target/archive.war`（`-ContextPath ROOT` 时为 `ROOT.war`）。

`-ContextPath` 决定 war 文件名，也就决定了 Tomcat 的访问上下文。**前端资源路径在构建时已写死**，修改上下文后必须重新执行本脚本，不能只改 war 文件名。

也可以手动执行：

```bash
mvn -f backend/pom.xml clean package -Pwar -DskipTests
```

### 3. 部署

1. 将 `archive.war` 复制到 `TOMCAT_HOME/webapps/`。
2. 如为重新部署，先删除旧的 `webapps/archive/` 解压目录，避免残留旧静态资源。
3. 给 Tomcat 进程配置环境变量（Linux 可写入 `bin/setenv.sh`，Windows 写入 `bin/setenv.bat`）：

```bash
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=archive_contract
export DB_USERNAME=archive_app
export DB_PASSWORD='******'
export JWT_SECRET='******'
export FILE_STORAGE_PATH=/opt/archive/data/uploads
export LOGGING_FILE_NAME=/opt/archive/logs/archive-system.log
```

4. 启动 Tomcat：

```bash
TOMCAT_HOME/bin/startup.sh      # Linux
TOMCAT_HOME\bin\startup.bat     # Windows
```

5. 访问 `http://server:8080/archive/`。

要点：

- 外部 Tomcat 部署时，`server.port` 与 `server.servlet.context-path` 配置不再生效，端口由 Tomcat 决定，上下文由 war 文件名决定。
- 附件目录务必使用绝对路径，并确保运行 Tomcat 的系统账号有读写权限。
- 日志使用 `LOGGING_FILE_NAME` 指向绝对路径，否则相对路径会落在 Tomcat 的工作目录。
- 若大附件上传失败，在 Tomcat `conf/server.xml` 的 Connector 上增加（默认 100MB）：

```xml
<Connector port="8080" protocol="HTTP/1.1"
           maxPostSize="104857600"
           maxSwallowSize="104857600" />
```

- 打包后的 war 仍可用 `java -jar archive.war --server.servlet.context-path=/archive` 在本机自测；如果不加 context-path 参数，前端资源会因路径不匹配而 404。

### 4. 验证

```bash
curl -I http://server:8080/archive/
curl -i http://server:8080/archive/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

根路径返回 `200 text/html`，接口返回 `code=200` 且包含 `token`；刷新 `/archive/contracts/12`、`/archive/ledger` 不应 404。

## 五、Nginx 反向代理（可选）

一体化 jar 已同时提供前端页面与 `/api` 接口，可直接对外服务。生产环境若需要域名、HTTPS 或统一入口，建议仅在前面加一层 Nginx 反向代理，不再单独托管静态目录。

### 1. Nginx 配置

```nginx
server {
    listen 80;
    server_name archive.example.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name archive.example.com;

    ssl_certificate     /etc/nginx/ssl/archive.crt;
    ssl_certificate_key /etc/nginx/ssl/archive.key;

    # 与后端 100MB 请求上限保持一致
    client_max_body_size 100m;

    # 页面、静态资源、接口全部转发给 Spring Boot
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_read_timeout    120s;
        proxy_send_timeout    120s;
    }

    # 构建产物文件名带内容哈希，可长期缓存
    location /assets/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

要点：

- 不需要配置 `root` 或 `try_files`：`/` 与 `/contracts`、`/ledger` 等前端路由由后端 `SpaController` 回退到 `index.html`，刷新不会 404。
- 不需要为 `/api` 单独配置 location，统一由 `location /` 转发即可；如按路径拆分，`proxy_pass` 后不要丢失 `/api` 前缀。
- `client_max_body_size` 不小于后端 `max-request-size`（默认 100MB）。
- 同域代理下前端无需关心 CORS；`CORS_ORIGINS` 仅用于前后端分域或本地 Vite 开发。
- 如启用 HTTPS，JWT 与附件下载均走加密链路。

### 2. 可选：前后端分离静态托管

如企业已有独立静态站点或 CDN，也可不使用 jar 内置前端，改为：

```bash
cd frontend
npm ci
npm run build
# 将 frontend/dist 发布到 /var/www/archive/dist
```

此时 Nginx 需使用 `root /var/www/archive/dist;` 与 `location / { try_files $uri $uri/ /index.html; }`，并仅把 `/api/` 代理到 `127.0.0.1:8080`。升级时需同时替换前端包与后端 jar；推荐仍采用一体化模式以简化发布。

## 六、文件与日志目录

| 目录 | 默认 | 说明 |
| --- | --- | --- |
| 附件 | `./data/uploads` | 必须持久化、纳入备份、磁盘监控 |
| 日志 | `./logs/archive-system.log` | Spring Boot 运行日志，按需接入日志轮转 |
| 前端 | jar/war 内 `BOOT-INF/classes/static` | 随包一起发布，无需单独维护；`frontend/dist` 仅用于分离部署 |

Linux 权限示例：

```bash
chown -R archive:archive /opt/archive/data /opt/archive/logs
chmod 750 /opt/archive/data /opt/archive/data/uploads
```

附件目录丢失会导致详情可查但预览/下载失败，必须与数据库一起备份。

## 七、备份与恢复

### 备份

```bash
mysqldump -uroot -p --single-transaction --routines --triggers \
  archive_contract > /backup/archive_contract_$(date +%F).sql

tar -czf /backup/archive_uploads_$(date +%F).tar.gz -C /opt/archive/data uploads
```

建议：

- 数据库每日全量 + binlog 增量。
- 附件目录每日增量、每周全量。
- 备份文件异地保存并定期做恢复演练。
- 备份账号只需 `SELECT`、`LOCK TABLES` 等最小权限。

### 恢复

1. 停止后端，避免恢复期间写入。
2. 恢复数据库备份。
3. 恢复附件目录到相同路径。
4. 校验触发器是否随备份恢复；如使用 `--triggers` 会包含触发器。
5. 启动后端并执行登录、附件预览、归档审核冒烟测试。

## 八、升级流程

1. 备份数据库与附件目录。
2. 记录当前版本与 jar 包。
3. 执行增量 SQL（禁止直接重跑 `01_schema.sql`）。
4. 用 `build-all.ps1` 生成新 jar，或用 `build-war.ps1 -ContextPath archive` 生成新 war。
5. 替换后端 jar 并重启服务；WAR 部署则替换 `webapps/archive.war` 并重启 Tomcat。前端已内嵌，无需单独替换 `dist`。
6. 验证根路径页面、登录、合同列表、归档审核、附件上传/预览下载、台账导出、预警扫描。
7. 如失败，回滚 jar/war 与数据库备份；如为分离部署，再一并回滚前端包。

## 九、上线检查清单

- [ ] `JWT_SECRET` 已替换为随机值，未写入代码仓库。
- [ ] 演示账号密码已修改或禁用。
- [ ] 数据库使用独立最小权限账号。
- [ ] 附件目录为绝对路径且已持久化、纳入备份。
- [ ] HTTPS 已启用，`CORS_ORIGINS` 仅包含可信域名。
- [ ] 6 个数据保护触发器存在。
- [ ] 已确认访问根路径可打开前端，刷新 `/contracts`、`/ledger` 等路由不 404。
- [ ] Nginx（如启用）已把 `/` 与 `/api` 全部代理到后端，`client_max_body_size`、`proxy_read_timeout` 满足大附件上传。
- [ ] WAR 部署使用 Tomcat 10.1.x，上下文路径与 `build-war.ps1 -ContextPath` 一致，`/archive/api/**` 可正常登录。
- [ ] Tomcat 的 `FILE_STORAGE_PATH`、`LOGGING_FILE_NAME` 为绝对路径且对 Tomcat 运行账号可写。
- [ ] 日志目录可写，磁盘告警已配置。
- [ ] 已验证合同全流程、借阅下载、导出、打印、权限隔离。
- [ ] 已制定数据库 + 附件联合备份与恢复演练计划。
