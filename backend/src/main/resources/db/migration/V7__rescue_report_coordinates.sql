-- RB-22: tọa độ của báo cáo cứu hộ nếu có phải hợp lệ, có đủ cả vĩ độ và kinh độ hoặc không có cả hai.
UPDATE rescue_reports SET latitude = NULL, longitude = NULL WHERE (latitude IS NULL) <> (longitude IS NULL);
ALTER TABLE rescue_reports
  ADD CONSTRAINT ck_report_coordinates CHECK (
    (latitude IS NULL AND longitude IS NULL)
    OR (latitude IS NOT NULL AND longitude IS NOT NULL
        AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180));
