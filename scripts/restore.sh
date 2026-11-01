#!/usr/bin/env bash
# Khôi phục database dev từ một bản sao lưu (NFR-06). Dữ liệu hiện tại sẽ bị thay thế.
#   scripts/restore.sh backups/aar-20260101-020000.dump
set -euo pipefail
cd "$(dirname "$0")/.."
file="${1:?Cần đường dẫn tệp .dump}"
[ -f "$file" ] || { echo "Không thấy tệp $file" >&2; exit 1; }
read -r -p "Thay toàn bộ dữ liệu database aar bằng $file? Gõ yes để tiếp tục: " ok
[ "$ok" = "yes" ] || { echo "Đã hủy"; exit 1; }
docker compose exec -T postgres psql -U aar -d postgres -v ON_ERROR_STOP=1 \
  -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'aar' AND pid <> pg_backend_pid();" \
  -c "DROP DATABASE IF EXISTS aar;" -c "CREATE DATABASE aar OWNER aar;"
docker compose exec -T postgres pg_restore -U aar -d aar --no-owner < "$file"
echo "Đã khôi phục từ $file. Khởi động lại backend để Flyway kiểm tra schema."
