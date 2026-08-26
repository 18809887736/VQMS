#!/bin/bash
# VQMS 增量部署 10.0.0.9 —— 一条命令跑完 传输→构建→重启→探活
# 用法: tools/deploy-update.sh [backend|nginx|all]   (默认 all)
#
# 只改后端传 backend、只改前端传 nginx,避免另一侧 26s+ 的无效重建。
# 注意: sql/*.sql 变更需要清卷重导(down -v),属破坏性操作,不在此脚本内,手动执行。
set -euo pipefail

SVC="${1:-all}"
case "$SVC" in backend|nginx|all) ;; *) echo "用法: $0 [backend|nginx|all]" >&2; exit 1;; esac

SSH=~/.ssh/ssh9.sh
cd "$(dirname "$0")/.."

T0=$SECONDS
log() { printf '[%3ds] %s\n' "$((SECONDS - T0))" "$*"; }

log "传输源码"
tar czf - --exclude=node_modules --exclude=target --exclude=dist --exclude=.git \
    --exclude='*.zip' --exclude='*.log' \
    RuoYi-Vue-springboot3 RuoYi-Vue3 sql docker-compose.yml .env.example mysql-charset.cnf \
  | "$SSH" 'mkdir -p ~/vqms && tar xzf - -C ~/vqms'

SVCS=""
if [ "$SVC" != nginx ]; then
  log "构建 backend"
  # 输出收进远端日志:成功只回 3 行,失败回 30 行定位
  "$SSH" 'cd ~/vqms && docker compose build backend > /tmp/vqms-bb.log 2>&1 \
    && tail -3 /tmp/vqms-bb.log || { tail -30 /tmp/vqms-bb.log; exit 1; }' < /dev/null
  SVCS="backend"
fi
if [ "$SVC" != backend ]; then
  log "构建 nginx"
  "$SSH" 'cd ~/vqms && docker compose build nginx > /tmp/vqms-ng.log 2>&1 \
    && tail -3 /tmp/vqms-ng.log || { tail -30 /tmp/vqms-ng.log; exit 1; }' < /dev/null
  SVCS="$SVCS nginx"
fi

log "滚动重启:$SVCS"
"$SSH" "cd ~/vqms && docker compose up -d $SVCS" < /dev/null

log "等待 backend 就绪(最多约 75s)"
for i in $(seq 1 30); do
  code=$("$SSH" "curl -s -o /dev/null -w '%{http_code}' --max-time 3 -X POST http://localhost:8080/prod-api/login" || echo 000)
  if [ "$code" != "000" ] && [ "$code" != "502" ]; then
    log "完成 — backend 就绪 (HTTP $code)"
    exit 0
  fi
  sleep 2
done
echo "超时未就绪,排查:" >&2
echo "  ~/.ssh/ssh9.sh 'cd ~/vqms && docker compose logs --tail 50 backend'" >&2
exit 1
