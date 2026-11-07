-- Đồng bộ nghiệp vụ với báo cáo bản chốt (rút gọn 30 trang).

-- 1. Bỏ cam kết triệt sản khi nộp đơn: checklist mục 5 đã xử lý thú chưa đủ tuổi triệt sản.
ALTER TABLE adoption_applications DROP COLUMN sterilization_pledge;

-- 2. Bỏ sự kiện Chuyển cơ sở: vòng đời chỉ còn hai sự kiện kết thúc là Qua đời và Thất lạc (mục 2.1.6).
ALTER TABLE pets DROP CONSTRAINT pets_status_check;
ALTER TABLE pets ADD CONSTRAINT pets_status_check
  CHECK (status IN ('IN_CARE','IN_TREATMENT','AVAILABLE','PENDING_HANDOVER','ADOPTED','DECEASED','LOST'));

ALTER TABLE pet_end_events DROP CONSTRAINT ck_end_transfer_dest;
ALTER TABLE pet_end_events DROP COLUMN destination_org;
ALTER TABLE pet_end_events DROP CONSTRAINT pet_end_events_event_type_check;
ALTER TABLE pet_end_events ADD CONSTRAINT pet_end_events_event_type_check
  CHECK (event_type IN ('DECEASED','LOST'));

-- 3. QĐ-14: hủy yêu cầu thú y vẫn ghi chi phí đã phát sinh (mặc định 0), tính vào tổng chi phí điều trị.
UPDATE vet_requests SET total_cost = 0 WHERE status = 'CANCELLED' AND total_cost IS NULL;
-- completed_at là thời điểm đóng yêu cầu (hoàn tất hoặc hủy), dùng cho báo cáo chi phí theo tháng
UPDATE vet_requests SET completed_at = requested_at WHERE status = 'CANCELLED' AND completed_at IS NULL;
ALTER TABLE vet_requests
  ADD CONSTRAINT ck_vet_cancelled_cost CHECK (status <> 'CANCELLED' OR total_cost IS NOT NULL);

-- 4. Tham số mới: đóng thu hồi (QĐ-18) và nhắc phân công ca (QĐ-17).
INSERT INTO system_parameters (param_key, param_value, description) VALUES
  ('RECALL_CLOSE_AFTER_DAYS',     '30', 'So ngay o Cho thu hoi truoc khi Quan ly duoc dong thu hoi'),
  ('CASE_UNASSIGNED_ALERT_HOURS', '2',  'Ca chua phan cong qua so gio nay vao bao cao ngoai le');
UPDATE system_parameters SET description = 'Tuoi toi thieu de triet san (tuan), chua du tuoi thi checklist muc 5 dat'
 WHERE param_key = 'MIN_STERILIZATION_AGE_WEEKS';
