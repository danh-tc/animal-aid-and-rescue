-- Hình thức liên hệ khi hậu kiểm (mục 2.1.5): lần 1 gọi điện, lần 2 thăm nhà hoặc gọi video.
-- Lần hậu kiểm đã thực hiện (DONE) bắt buộc ghi hình thức.
ALTER TABLE follow_ups
  ADD COLUMN contact_method VARCHAR(12)
  CHECK (contact_method IN ('PHONE', 'HOME_VISIT', 'VIDEO_CALL'));

-- Dữ liệu cũ: lần 1 coi là gọi điện, các lần sau là thăm nhà
UPDATE follow_ups
   SET contact_method = CASE WHEN sequence_no = 1 THEN 'PHONE' ELSE 'HOME_VISIT' END
 WHERE status = 'DONE';

ALTER TABLE follow_ups
  ADD CONSTRAINT ck_follow_up_done_contact CHECK (status <> 'DONE' OR contact_method IS NOT NULL);
