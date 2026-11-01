#!/usr/bin/env bash
# Sao lưu ngay database dev ra backups/ (NFR-06). Dùng khi cần một bản trước thao tác rủi ro.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p backups
file="backups/aar-$(date +%Y%m%d-%H%M%S).dump"
docker compose exec -T postgres pg_dump -U aar -d aar --format=custom --no-owner > "$file"
echo "Đã sao lưu: $file"
