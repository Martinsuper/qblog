# MySQL 数据库配置指南

## 1. 初始化数据库

### 使用 Docker（推荐）
数据库会通过 docker-compose 自动初始化，无需手动操作。

### 手动初始化（不使用 Docker 时）
```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source /path/to/qblog/database/mysql-schema.sql
```
