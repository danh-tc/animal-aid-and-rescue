#!/bin/sh




set -eu
BACKUP_AT="${BACKUP_AT:-02:00}"
KEEP="${BACKUP_KEEP_DAYS:-7}"

backup() {
  stamp="$(date +%Y%m%d-%H%M%S)"
  file="/backups/aar-$stamp.dump"
  files="/backups/aar-$stamp-uploads.tar.gz"
  if pg_dump --format=custom --no-owner --file="$file.part"; then
    mv "$file.part" "$file"
    echo "$(date '+%F %T') sao lưu database xong: $file"
  else
    rm -f "$file.part"
    echo "$(date '+%F %T') sao lưu database THẤT BẠI" >&2
  fi
  if [ -d /uploads ] && tar -czf "$files.part" -C /uploads .; then
    mv "$files.part" "$files"
    echo "$(date '+%F %T') sao lưu tệp đính kèm xong: $files"
  else
    rm -f "$files.part"
    echo "$(date '+%F %T') sao lưu tệp đính kèm THẤT BẠI hoặc thiếu /uploads" >&2
  fi
  find /backups \( -name 'aar-*.dump' -o -name 'aar-*-uploads.tar.gz' \) -mtime +"$KEEP" -delete
}

backup
while true; do
  now=$(date +%s)
  target=$(date -d "$(date +%F) $BACKUP_AT" +%s 2>/dev/null || date -D '%Y-%m-%d %H:%M' -d "$(date +%F) $BACKUP_AT" +%s)
  [ "$target" -le "$now" ] && target=$((target + 86400))
  sleep $((target - now))
  backup
done
