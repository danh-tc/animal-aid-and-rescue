#!/usr/bin/env bash
# Sao lưu ngay database dev và thư mục tệp đính kèm ra backups/ (NFR-06).
# UPLOADS_DIR mặc định backend/data/uploads, khớp STORAGE_DIR khi chạy backend trong thư mục backend.
set -euo pipefail
cd "$(dirname "$0")/.."
UPLOADS_DIR="${UPLOADS_DIR:-backend/data/uploads}"
mkdir -p backups
stamp="$(TZ=Asia/Ho_Chi_Minh date +%Y%m%d-%H%M%S)"
docker compose exec -T postgres pg_dump -U aar -d aar --format=custom --no-owner > "backups/aar-$stamp.dump"
echo "Đã sao lưu database: backups/aar-$stamp.dump"
if [ -d "$UPLOADS_DIR" ]; then
  tar -czf "backups/aar-$stamp-uploads.tar.gz" -C "$UPLOADS_DIR" .
  echo "Đã sao lưu tệp đính kèm: backups/aar-$stamp-uploads.tar.gz"
else
  echo "Không thấy $UPLOADS_DIR, bỏ qua tệp đính kèm" >&2
fi
