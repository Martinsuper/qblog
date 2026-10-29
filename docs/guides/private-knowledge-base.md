# 私人知识库实施与部署

## 实现范围

- 默认只有管理员可登录并访问私人共享知识工作区。关闭公开注册；旧博客读接口、API 文档和 readiness 也需要管理员认证。只有 POST `/api/auth/login` 与 GET `/api/health` 公开。
- 正式首页为知识库，支持 01 留白三栏、03 纸间卡片、06 笔记书架、07 目录知识库。四布局共用后端数据，浅色/深色/跟随系统配色独立保存本机偏好。
- 使用 Tiptap 正式文档编辑器，Markdown 存储，表格、代码、撤销与重做。07 直接编辑；800ms 停顿自动保存、Ctrl/Cmd S 保存，中文输入组合期间不发自动保存。
- 服务端分页、标题/正文关键词搜索、来源和标签过滤、收藏、层级目录、收件箱、移动和删除。
- 每篇笔记有 version，更新和删除执行乐观锁。409 冲突暂停自动保存，允许下载草稿后载入最新版本，不静默覆盖 MCP 或其他窗口修改。
- MCP 独立 Token：仅一次显示完整凭证，服务器仅 SHA-256 摘要；可撤销、有效期<=365天、READ/WRITE 与目录精确范围。WRITE 不隐含 READ。目录范围不自动包含子目录；收件箱单独授权。
- MCP Streamable HTTP，JSON 响应、stateless、协议 2025-03-26，工具 `notes_search` / `notes_get` / `notes_create` / `notes_update`。
- 当前检索为 SQL 关键词查询，不是向量检索或 AI 问答；界面索引状态明确“未配置”。

## 数据与迁移

新增独立 `knowledge_workspace` / `knowledge_folder` / `knowledge_note` / `knowledge_api_token` 表。旧 `article` / `category` 表不删除、不回写，旧文章含草稿复制到单一管理员工作区；软删除保留为 tombstone。此工作区不是多租户产品：多个管理员共享同一份知识库。

**先备份，再迁移。不要直接在未知生产数据库上运行。** 新程序启动前，已有环境依次确认 V1/V2/V3 已执行，再运行 V4、V5：

```bash
# 根据实际数据库容器名、账号与密码替换；避免密码进入共享日志。
docker exec qblog-mysql mysqldump -u root -p qblog > qblog-before-knowledge.sql

docker exec -i qblog-mysql mysql -u root -p qblog < database/migrations/V4__knowledge_base.sql
docker exec -i qblog-mysql mysql -u root -p qblog < database/migrations/V5__knowledge_api_tokens.sql
```

注意：交互式 `-p` 与 stdin 文件同时使用时应通过安全 mysql option file / MYSQL_PWD 环境或运维密钥注入方式提供密码，不在命令行明文拼接。上述是迁移文件与命令结构示意。

- V4 需要已有旧表结构（全新安装先执行 `database/mysql-schema.sql` 以及适用的迁移）。该初始化脚本已包含 `user.password_version`，不要再对其执行 V3；已有数据库应先检查列是否存在，再决定是否执行 V3。
- MySQL DDL 会自动提交；V4 数据复制部分单独事务。脚本可重复执行，已迁移 ID 不覆盖已编辑内容；**上线后不要继续通过旧后台修改旧文章表**，两份表不做双向同步。
- 迁移后核对目录和笔记数量、草稿访问限制、标签和正文。备份恢复也必须包含新的 knowledge 表，不能依赖旧 JSON 博客备份覆盖知识库数据。
- 建议首次上线前关闭旧博客部署，防止旧版本继续公开数据库内容。

## 构建和运行

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"
(cd backend && mvn test && mvn package)
(cd frontend && npm ci && npm test && npm run build)
```

生产继续使用现有后端部署与 nginx 静态资源方案。必须配置强 JWT_SECRET、修改初始管理员密码并使用 HTTPS。CORS_ALLOWED_ORIGINS 设置为真实前端 origin（精确协议+域名+端口，不含路径），否则有 Origin 的 MCP 浏览器客户端将被拒绝。

访问网站后登录管理员，首页进入知识库。`/settings/tokens` 新建 Token；只选需要的目录与操作范围。Token 不放 URL、不放公开源码、不复用浏览器 JWT。

## MCP 客户端配置示意

```json
{
  "mcpServers": {
    "younote": {
      "type": "http",
      "url": "https://YOUR_HOST/api/mcp",
      "headers": {
        "Authorization": "Bearer YOUR_ONE_TIME_TOKEN"
      }
    }
  }
}
```

不同客户端字段可能不同，选择 Streamable HTTP remote server。服务器没有 OAuth 动态注册；使用自定义 Authorization 头。无 Origin 的 CLI 可连接，有 Origin 的请求必须在 CORS 白名单。GET SSE 不提供，返回 405；POST 返回 JSON。

初始化可用：

```bash
curl https://YOUR_HOST/api/mcp \
  -H "Authorization: Bearer $YOUNOTE_TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  --data '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"verification","version":"1.0"}}}'
```

创建工具参数：`title`, `content`, `folderId`（null为收件箱）, `tags`, `externalId`。同 Token 内稳定 externalId 支持幂等重试；内容不同时必须提供最新 version，否则返回工具错误。更新是完整替换，必须包含 title/content/version，省略 folderId 意味收件箱，**不是 PATCH**。先读取当前笔记再合并更新。Token 命名空间不同，不共享 externalId。

## 验证边界

- 自动化测试覆盖真实 Spring Security filter chain、Token 生命周期/摘要、跨目录 scope、MCP JSON-RPC、来源身份、版本冲突与保存排队竞态。
- 浏览器可以使用受控 API 契约响应验证 Vue/Tiptap 行为；这不等于真实 MySQL 端到端测试。
- 已在隔离的 MySQL 8.0 容器验证 V4、V5 可重复执行，并用样例验证目录、标签、收藏和删除状态迁移，以及重复迁移不覆盖已编辑笔记。尚未对用户实际数据库执行迁移或部署生产；上线前仍需在对应的测试数据库核对真实数据。
- `%关键词%` 扫描和共享工作区写锁适合初版正确性，不代表已验证百万级并发性能。达到更高规模时按真实数据量引入全文/向量索引与更细粒度锁。

## MCP 安装与使用教程

1. 以管理员登录网站，打开“API 与 MCP”，进入“访问令牌”，创建一个有明确用途的令牌。选择读取或写入权限，并勾选所需的目录或收件箱。令牌只在创建时完整显示一次。
2. 在支持 Streamable HTTP 和自定义请求头的 MCP 客户端中新增服务。地址使用页面显示的实际地址，请求头为 `Authorization: Bearer <令牌>`。上方 JSON 示例是一种客户端配置格式；具体字段以客户端文档为准。远程连接应使用 HTTPS。
3. 重启或重新连接客户端，确认可以看到 `notes_search`、`notes_get`、`notes_create` 和 `notes_update`。先尝试搜索所授权目录中的笔记。
4. 写入时确认令牌有写权限，目标目录在授权范围内。更新前先读取当前笔记和版本；目录权限不自动包含子目录，收件箱需要单独授权。
5. 如果连接失败，检查服务地址能否从客户端访问、令牌是否过期或撤销、权限与目录范围是否匹配，以及客户端是否支持自定义 Authorization 请求头。浏览器客户端的来源还须在服务端 CORS 白名单中。

令牌相当于访问凭证，不要提交到仓库或粘贴到公开场所；泄露后在令牌列表中立即撤销并重新创建。文章工具包括 `articles_search`、`articles_get`、`articles_create`、`articles_update` 、`articles_publish` 和 `articles_delete`。更新或发布前读取最新版本并传入 `version`；发生冲突后重新读取并合并。创建默认为草稿，发布及修改已发布文章需要独立的发布权限，删除需要单独授权并执行软删除。

## 文章 MCP 迁移

升级现有数据库前先备份并执行 `database/migrations/V6__article_mcp.sql`，再部署前后端；此迁移需要执行一次。全新数据库的 `database/mysql-schema.sql` 已包含文章 version 列，应跳过 V6 中的 ALTER，仅执行 V6 的建表语句。所有旧文章更新客户端必须在更新请求中传入读取时获得的 `version`。调用记录保留工具名称、令牌 ID、结果、耗时和时间，不保存正文或凭证，页面显示最近 100 条。
