#!/bin/sh
# Chạy trong container "backup" của docker-compose: sao lưu một lần khi khởi động,
# sau đó mỗi ngày vào giờ BACKUP_AT, xóa bản cũ hơn BACKUP_KEEP_DAYS ngày (NFR-06).
set -eu
BACKUP_AT="${BACKUP_AT:-02:00}"
KEEP="${BACKUP_KEEP_DAYS:-7}"

dump() {
  file="/backups/aar-$(date +%Y%m%d-%H%M%S).dump"
  if pg_dump --format=custom --no-owner --file="$file.part"; then
    mv "$file.part" "$file"
    echo "$(date '+%F %T') sao lưu xong: $file"
  else
    rm -f "$file.part"
    echo "$(date '+%F %T') sao lưu THẤT BẠI" >&2
  fi
  find /backups -name 'aar-*.dump' -mtime +"$KEEP" -delete
}

dump
while true; do
  now=$(date +%s)
  target=$(date -d "$(date +%F) $BACKUP_AT" +%s 2>/dev/null || date -D '%Y-%m-%d %H:%M' -d "$(date +%F) $BACKUP_AT" +%s)
  [ "$target" -le "$now" ] && target=$((target + 86400))
  sleep $((target - now))
  dump
done
