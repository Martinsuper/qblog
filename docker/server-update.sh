#!/bin/bash
# QBlog 服务器端更新脚本（部署在 /opt/qblog/update.sh，由本地 deploy.sh 同步过来）
# 用法：
#   /opt/qblog/update.sh               # 应用 artifacts/ 下的新镜像与新前端，跑增量迁移并重启
#   /opt/qblog/update.sh --restart-only # 仅重启服务（不更新任何文件）
#
# artifacts 目录约定：
#   /opt/qblog/artifacts/backend-image.tar.gz   docker save 导出的后端镜像
#   /opt/qblog/artifacts/frontend-dist.tar.gz   前端构建产物
#   （文件存在才应用对应更新，均可缺省）

set -e

BASE=/opt/qblog
ART=$BASE/artifacts
COMPOSE_DIR=$BASE/docker
COMPOSE="docker compose -p qblog -f $COMPOSE_DIR/docker-compose.server.yml --env-file $COMPOSE_DIR/.env"
RESTART_ONLY=0
[ "${1:-}" = "--restart-only" ] && RESTART_ONLY=1

# 已在全新 schema 中生效的历史迁移（当前 mysql-schema.sql 已包含其结果，标记为已应用）
BOOTSTRAP_VERSIONS="V1__add_indexes_and_constraints.sql V2__remove_like_comment_features.sql V3__add_user_password_version.sql"

step() { echo ""; echo "==> $1"; }

if [ "$RESTART_ONLY" = "1" ]; then
    step "仅重启服务..."
    $COMPOSE up -d --force-recreate backend
else
    if [ -f "$ART/backend-image.tar.gz" ]; then
        step "加载新后端镜像..."
        gunzip -c "$ART/backend-image.tar.gz" | docker load
    else
        step "无新后端镜像，跳过"
    fi

    if [ -f "$ART/frontend-dist.tar.gz" ]; then
        step "更新前端静态文件..."
        rm -rf "$BASE/frontend/dist.new" "$BASE/frontend/dist.old"
        mkdir -p "$BASE/frontend/dist.new"
        tar xzf "$ART/frontend-dist.tar.gz" -C "$BASE/frontend/dist.new"
        [ -d "$BASE/frontend/dist" ] && mv "$BASE/frontend/dist" "$BASE/frontend/dist.old"
        mv "$BASE/frontend/dist.new" "$BASE/frontend/dist"
        echo "    前端已更新（旧版本备份在 dist.old）"
    else
        step "无新前端产物，跳过"
    fi

    step "应用数据库增量迁移..."
    DB_ROOT_PASSWORD=$(grep "^DB_ROOT_PASSWORD=" "$COMPOSE_DIR/.env" | cut -d= -f2)
    MYSQL="docker exec qblog-mysql mysql -u root -p$DB_ROOT_PASSWORD --default-character-set=utf8mb4"
    $MYSQL -e "CREATE TABLE IF NOT EXISTS qblog._schema_migrations (version VARCHAR(128) PRIMARY KEY, applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);" 2>/dev/null
    for v in $BOOTSTRAP_VERSIONS; do
        $MYSQL -N -e "INSERT IGNORE INTO qblog._schema_migrations (version) VALUES ('$v');" 2>/dev/null
    done
    if [ -d "$BASE/database/migrations" ]; then
        for f in $(ls "$BASE/database/migrations"/*.sql 2>/dev/null | sort); do
            v=$(basename "$f")
            APPLIED=$($MYSQL -N -e "SELECT COUNT(*) FROM qblog._schema_migrations WHERE version='$v';" 2>/dev/null | tr -d '[:space:]')
            if [ "$APPLIED" = "0" ]; then
                echo "    应用迁移: $v"
                docker exec -i qblog-mysql mysql -u root -p"$DB_ROOT_PASSWORD" --default-character-set=utf8mb4 qblog < "$f"
                $MYSQL -e "INSERT INTO qblog._schema_migrations (version) VALUES ('$v');" 2>/dev/null
            fi
        done
        echo "    迁移检查完成"
    fi

    step "更新容器..."
    $COMPOSE up -d
fi

step "等待后端健康检查..."
OK=0
for i in $(seq 1 40); do
    if curl -sf http://127.0.0.1:8081/api/health > /dev/null 2>&1; then OK=1; break; fi
    sleep 3
done
if [ "$OK" = "1" ]; then
    echo "    ✅ 后端已就绪: $(curl -s http://127.0.0.1:8081/api/health)"
else
    echo "    ❌ 后端 120 秒内未就绪，最近日志："
    docker logs --tail 15 qblog-backend 2>&1 | sed 's/^/    /'
    docker exec qblog-backend sh -c "tail -15 logs/qblog.log 2>/dev/null" | sed 's/^/    /'
    exit 1
fi

step "容器状态"
docker compose -p qblog -f $COMPOSE_DIR/docker-compose.server.yml ps --format "table {{.Name}}\t{{.Status}}"
echo ""
echo "✅ 更新完成，访问 https://bqlog.younote.top"
