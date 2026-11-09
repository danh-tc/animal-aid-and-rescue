#!/usr/bin/env bash

#   scripts/restore.sh backups/aar-20260101-020000.dump


set -euo pipefail
cd "$(dirname "$0")/.."
file="${1:?Cần đường dẫn tệp .dump}"
[ -f "$file" ] || { echo "Không thấy tệp $file" >&2; exit 1; }
UPLOADS_DIR="${UPLOADS_DIR:-backend/data/uploads}"
archive="${file%.dump}-uploads.tar.gz"
[ -f "$archive" ] && extra=" và thư mục ảnh $UPLOADS_DIR bằng $archive" || extra=""
read -r -p "Thay toàn bộ dữ liệu database aar bằng $file$extra? Gõ yes để tiếp tục: " ok
[ "$ok" = "yes" ] || { echo "Đã hủy"; exit 1; }

docker compose exec -T postgres psql -U aar -d postgres -v ON_ERROR_STOP=1 \
  -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'aar' AND pid <> pg_backend_pid();" \
  -c "DROP DATABASE IF EXISTS aar;" -c "CREATE DATABASE aar OWNER aar;"
docker compose exec -T postgres pg_restore -U aar -d aar --no-owner < "$file"
echo "Đã khôi phục database từ $file"

if [ -f "$archive" ]; then
  if [ -d "$UPLOADS_DIR" ]; then
    old="$UPLOADS_DIR.truoc-khoi-phuc-$(date +%Y%m%d-%H%M%S)"
    mv "$UPLOADS_DIR" "$old"
    echo "Đã giữ thư mục ảnh cũ ở $old"
  fi
  mkdir -p "$UPLOADS_DIR"
  tar -xzf "$archive" -C "$UPLOADS_DIR"
  echo "Đã khôi phục tệp đính kèm từ $archive"
else
  echo "Không có $archive, giữ nguyên thư mục ảnh hiện tại" >&2
fi
echo "Khởi động lại backend để Flyway kiểm tra schema."
