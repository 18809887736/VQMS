#!/bin/bash
# 首次初始化时自动执行（docker-entrypoint-initdb.d 机制）
# 建一个最小权限的应用账号，后端 datasource 用它连库，不用 root
# 对应 项目规划_v3_1.md §9 "数据库账号分离（最小权限）"
set -e

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
    CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASSWORD}';
    GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.* TO '${DB_USER}'@'%';
    FLUSH PRIVILEGES;
EOSQL

echo "已创建应用账号 ${DB_USER}，仅授予 ${MYSQL_DATABASE} 库的增删改查权限（无 DDL/GRANT/跨库权限）"
