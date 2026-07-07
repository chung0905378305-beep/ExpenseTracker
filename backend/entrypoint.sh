#!/bin/sh
set -e

# 数据库文件路径
DB_DIR="/app/prisma/data"
DB_FILE="$DB_DIR/dev.db"

# 如果数据库文件不存在，运行迁移
if [ ! -f "$DB_FILE" ]; then
  echo ">>> 初始化数据库..."
  mkdir -p "$DB_DIR"
  npx prisma migrate deploy
  echo ">>> 数据库初始化完成"
fi

echo ">>> 启动会员管理后台..."
exec node dist/index.js
