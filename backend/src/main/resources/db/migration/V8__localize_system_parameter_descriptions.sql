UPDATE system_parameters
SET description = CASE param_key
  WHEN 'MIN_ADOPTION_AGE_WEEKS' THEN 'Tuổi tối thiểu để tách mẹ (tuần)'
  WHEN 'MIN_STERILIZATION_AGE_WEEKS' THEN 'Tuổi tối thiểu để triệt sản (tuần); nếu chưa đủ tuổi thì checklist mục 5 vẫn đạt'
  WHEN 'QUARANTINE_DAYS' THEN 'Số ngày cách ly theo dõi'
  WHEN 'FOLLOW_UP_1_DAYS' THEN 'Hậu kiểm lần 1 sau bàn giao (ngày)'
  WHEN 'FOLLOW_UP_2_DAYS' THEN 'Hậu kiểm lần 2 sau bàn giao (ngày)'
  WHEN 'APPLICATION_ALERT_DAYS' THEN 'Đơn chờ quá số ngày này sẽ xuất hiện trong báo cáo ngoại lệ'
  WHEN 'REPORT_RATE_LIMIT_PER_HOUR' THEN 'Số báo cáo tối đa cho mỗi số điện thoại trong một giờ'
  WHEN 'MAX_PENDING_APPLICATIONS' THEN 'Số đơn đang chờ tối đa của một người nhận nuôi'
  WHEN 'RECALL_CLOSE_AFTER_DAYS' THEN 'Số ngày ở trạng thái Chờ thu hồi trước khi Quản lý được đóng thu hồi'
  WHEN 'CASE_UNASSIGNED_ALERT_HOURS' THEN 'Ca chưa phân công quá số giờ này sẽ xuất hiện trong báo cáo ngoại lệ'
END
WHERE param_key IN (
  'MIN_ADOPTION_AGE_WEEKS',
  'MIN_STERILIZATION_AGE_WEEKS',
  'QUARANTINE_DAYS',
  'FOLLOW_UP_1_DAYS',
  'FOLLOW_UP_2_DAYS',
  'APPLICATION_ALERT_DAYS',
  'REPORT_RATE_LIMIT_PER_HOUR',
  'MAX_PENDING_APPLICATIONS',
  'RECALL_CLOSE_AFTER_DAYS',
  'CASE_UNASSIGNED_ALERT_HOURS'
);
