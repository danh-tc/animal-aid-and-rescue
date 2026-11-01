-- Cập nhật nghiệp vụ sau rà soát báo cáo.

-- Checklist mục 1 "lần khám gần nhất kết luận ổn": yêu cầu thú y Hoàn tất phải có kết luận sức khỏe
ALTER TABLE vet_requests ADD COLUMN health_ok BOOLEAN;
UPDATE vet_requests SET health_ok = TRUE WHERE status = 'COMPLETED';
ALTER TABLE vet_requests
  ADD CONSTRAINT ck_vet_completed_health CHECK (status <> 'COMPLETED' OR health_ok IS NOT NULL);

-- Mục 2.1.4 bước 3: đơn được duyệt thì hẹn lịch bàn giao
ALTER TABLE adoption_applications ADD COLUMN handover_date DATE;

-- QĐ-08: mỗi người nhận nuôi có tối đa số đơn đang chờ này
INSERT INTO system_parameters (param_key, param_value, description) VALUES
  ('MAX_PENDING_APPLICATIONS', '5', 'So don dang cho toi da cua mot nguoi nhan nuoi');
