

DO $seed$
BEGIN
IF EXISTS (SELECT 1 FROM users) THEN
  RETURN;
END IF;

INSERT INTO users (id, email, password_hash, full_name, phone, role, address) VALUES
  (1, 'manager@aar.local',  '$2a$10$J54Egu6VIscbm0Ocio7r4.y3kgC9HeCRmwjoyiw3jf0Rwt.EO/cYy', 'Nguyễn Văn Quân', '0900000001', 'MANAGER', NULL),
  (2, 'staff1@aar.local',   '$2a$10$J54Egu6VIscbm0Ocio7r4.y3kgC9HeCRmwjoyiw3jf0Rwt.EO/cYy', 'Trần Thị Lan',    '0900000002', 'STAFF', NULL),
  (3, 'staff2@aar.local',   '$2a$10$J54Egu6VIscbm0Ocio7r4.y3kgC9HeCRmwjoyiw3jf0Rwt.EO/cYy', 'Lê Minh Tuấn',    '0900000003', 'STAFF', NULL),
  (4, 'adopter1@aar.local', '$2a$10$J54Egu6VIscbm0Ocio7r4.y3kgC9HeCRmwjoyiw3jf0Rwt.EO/cYy', 'Phạm Thu Hà',     '0900000004', 'ADOPTER', '12 Nguyễn Thị Minh Khai, Quận 1'),
  (5, 'adopter2@aar.local', '$2a$10$J54Egu6VIscbm0Ocio7r4.y3kgC9HeCRmwjoyiw3jf0Rwt.EO/cYy', 'Võ Quốc Bảo',     '0900000005', 'ADOPTER', 'Căn hộ 8.05, Chung cư Sky, Quận 2'),
  (6, 'adopter3@aar.local', '$2a$10$J54Egu6VIscbm0Ocio7r4.y3kgC9HeCRmwjoyiw3jf0Rwt.EO/cYy', 'Đặng Ngọc Mai',   '0900000006', 'ADOPTER', '45 Lê Văn Sỹ, Quận 3');

INSERT INTO vet_clinics (id, name, address, phone) VALUES
  (1, 'Phòng khám thú y An Bình',  '12 Đường số 3, Quận 7',      '0280000001'),
  (2, 'Bệnh viện thú y Bình Minh', '45 Đường Lê Lợi, Quận 1',     '0280000002');

INSERT INTO rescue_reports (id, code, reporter_name, reporter_phone, location_text, description, status, handled_by, created_at, handled_at) VALUES
  (1,  'BC-7K2M9Q', 'Hoàng Anh',  '0911000001', 'Gần chợ Bến Thành, Quận 1',      'Chó con bị thương ở chân sau, nằm cạnh sạp trái cây', 'NEW',       NULL, now() - interval '2 hours', NULL),
  (2,  'BC-H4T8WX', 'Mai Linh',   '0911000002', 'Công viên Gia Định, Gò Vấp',     'Mèo gầy, mắt bị đổ ghèn, có vẻ bị bỏ rơi',            'NEW',       NULL, now() - interval '30 minutes', NULL),
  (3,  'BC-3PZR6N', 'Quốc Huy',   '0911000003', 'Hẻm 120 Nguyễn Trãi, Quận 5',    'Chó đen đi lạc nhiều ngày',                           'PROCESSED', 2, now() - interval '3 days', now() - interval '3 days'),
  (4,  'BC-9VDL2F', 'Thanh Trúc', '0911000004', 'Khu chung cư Sky, Quận 2',       'Hai mèo con trong thùng giấy',                        'PROCESSED', 2, now() - interval '10 days', now() - interval '10 days'),
  (5,  'BC-M6Y3JA', 'Ẩn danh',    '0911000005', 'Không rõ',                       'Nội dung không liên quan',                            'INVALID',   3, now() - interval '5 days', now() - interval '5 days'),
  (6,  'BC-Q2W7EC', 'Bảo Ngọc',   '0911000006', 'Cầu Ánh Sao, Quận 7',            'Chó vàng trốn dưới gầm cầu',                          'PROCESSED', 3, now() - interval '1 day', now() - interval '1 day'),
  (7,  'BC-R8N4KD', 'Văn Tài',    '0911000007', 'Bến xe Miền Đông',               'Chó vàng lông ngắn đi lạc',                           'PROCESSED', 2, now() - interval '60 days', now() - interval '60 days'),
  (8,  'BC-T5J9PB', 'Thu Trang',  '0911000008', 'Đường Võ Văn Tần, Quận 3',       'Mèo mướp bị kẹt trên mái tôn',                        'PROCESSED', 2, now() - interval '55 days', now() - interval '55 days'),
  (9,  'BC-W3C7LV', 'Minh Khoa',  '0911000009', 'Khu công nghiệp Tân Bình',       'Chó đốm bị xích bỏ lại',                              'PROCESSED', 3, now() - interval '50 days', now() - interval '50 days'),
  (10, 'BC-Y6F2HR', 'Lan Phương', '0911000010', 'Chợ Bà Chiểu, Bình Thạnh',       'Mèo trắng bị thương ở tai',                           'PROCESSED', 3, now() - interval '90 days', now() - interval '90 days'),
  (11, 'BC-Z9G5MT', 'Đức Anh',    '0911000011', 'Đường Phạm Văn Đồng, Thủ Đức',   'Chó nâu bị xe quẹt',                                  'PROCESSED', 2, now() - interval '80 days', now() - interval '80 days'),
  (12, 'BC-B4X8QN', 'Hải Yến',    '0911000012', 'Hẻm 50 Lý Thường Kiệt, Quận 10', 'Mèo tam thể yếu, bỏ ăn',                              'PROCESSED', 3, now() - interval '20 days', now() - interval '20 days');

INSERT INTO rescue_cases (id, location_text, assigned_staff_id, status, failure_reason, created_by, created_at, closed_at) VALUES
  (1,  'Hẻm 120 Nguyễn Trãi, Quận 5',    2, 'SUCCEEDED',   NULL, 2, now() - interval '3 days',  now() - interval '3 days'),
  (2,  'Khu chung cư Sky, Quận 2',       2, 'SUCCEEDED',   NULL, 2, now() - interval '10 days', now() - interval '10 days'),
  (3,  'Khu chung cư Sky, Quận 2',       2, 'FAILED',      'Mèo con thứ hai chạy vào cống, không tiếp cận được', 2, now() - interval '10 days', now() - interval '10 days'),
  (4,  'Cầu Ánh Sao, Quận 7',            3, 'IN_PROGRESS', NULL, 3, now() - interval '1 day',   NULL),
  (5,  'Bến xe Miền Đông',               2, 'SUCCEEDED',   NULL, 2, now() - interval '60 days', now() - interval '60 days'),
  (6,  'Đường Võ Văn Tần, Quận 3',       2, 'SUCCEEDED',   NULL, 2, now() - interval '55 days', now() - interval '55 days'),
  (7,  'Khu công nghiệp Tân Bình',       3, 'SUCCEEDED',   NULL, 3, now() - interval '50 days', now() - interval '50 days'),
  (8,  'Chợ Bà Chiểu, Bình Thạnh',       3, 'SUCCEEDED',   NULL, 3, now() - interval '90 days', now() - interval '90 days'),
  (9,  'Đường Phạm Văn Đồng, Thủ Đức',   2, 'SUCCEEDED',   NULL, 2, now() - interval '80 days', now() - interval '80 days'),
  (10, 'Hẻm 50 Lý Thường Kiệt, Quận 10', 3, 'SUCCEEDED',   NULL, 3, now() - interval '20 days', now() - interval '20 days');

INSERT INTO report_cases (report_id, case_id) VALUES
  (3, 1), (4, 2), (4, 3), (6, 4), (7, 5), (8, 6), (9, 7), (10, 8), (11, 9), (12, 10);

INSERT INTO pets (id, code, name, species, sex, estimated_birth_date, color, description, status, area, current_location,
                  is_vaccinated, is_dewormed, is_sterilized, temperament_ok, eligibility_checked_at, eligibility_checked_by, created_at) VALUES
  (1, 'TH-2026-0001', 'Mực',  'DOG', 'MALE',    current_date - 300, 'Đen',        'Mới cứu, đang cách ly theo dõi',          'IN_CARE',          'QUARANTINE', 'Trung tâm', FALSE, FALSE, FALSE, FALSE, NULL, NULL, now() - interval '3 days'),
  (2, 'TH-2026-0002', 'Bông', 'CAT', 'FEMALE',  current_date - 70,  'Trắng xám',  'Nhiễm trùng mắt, đang lưu trú điều trị', 'IN_TREATMENT',     'QUARANTINE', 'Bệnh viện thú y Bình Minh', FALSE, TRUE, FALSE, TRUE, NULL, NULL, now() - interval '10 days'),
  (3, 'TH-2026-0003', 'Vàng', 'DOG', 'MALE',    current_date - 500, 'Vàng',       'Hiền, quen người, có hai đơn đang chờ',   'AVAILABLE',        'GENERAL',    'Trung tâm', TRUE, TRUE, TRUE, TRUE, now() - interval '20 days', 2, now() - interval '60 days'),
  (4, 'TH-2026-0004', 'Mướp', 'CAT', 'FEMALE',  current_date - 400, 'Mướp',       'Thích leo trèo',                           'AVAILABLE',        'GENERAL',    'Trung tâm', TRUE, TRUE, TRUE, TRUE, now() - interval '25 days', 2, now() - interval '55 days'),
  (5, 'TH-2026-0005', 'Đốm',  'DOG', 'FEMALE',  current_date - 700, 'Trắng đốm đen', 'Đã có đơn được duyệt, chờ bàn giao',    'PENDING_HANDOVER', 'GENERAL',    'Trung tâm', TRUE, TRUE, TRUE, TRUE, now() - interval '30 days', 3, now() - interval '50 days'),
  (6, 'TH-2026-0006', 'Miu',  'CAT', 'MALE',    current_date - 600, 'Trắng',      'Đã nhận nuôi, đang hậu kiểm',             'ADOPTED',          NULL,         'Nhà chủ nuôi', TRUE, TRUE, TRUE, TRUE, now() - interval '60 days', 3, now() - interval '90 days'),
  (7, 'TH-2026-0007', 'Lu',   'DOG', 'MALE',    current_date - 900, 'Nâu',        'Từng được nhận nuôi, chủ nuôi trả lại',  'IN_CARE',          'QUARANTINE', 'Trung tâm', TRUE, TRUE, TRUE, TRUE, NULL, NULL, now() - interval '80 days'),
  (8, 'TH-2026-0008', 'Tí',   'CAT', 'UNKNOWN', current_date - 200, 'Tam thể',    'Suy kiệt nặng khi được cứu',              'DECEASED',         NULL,         NULL, FALSE, FALSE, FALSE, FALSE, NULL, NULL, now() - interval '20 days');

INSERT INTO intakes (id, pet_id, source, rescue_case_id, returned_handover_id, intake_at, reason, priority, initial_assessment, created_by) VALUES
  (1, 1, 'RESCUE', 1,  NULL, now() - interval '3 days',  'Đi lạc',             'NEED_CHECK', 'Gầy, ve rận nhiều',          2),
  (2, 2, 'RESCUE', 2,  NULL, now() - interval '10 days', 'Bị bỏ trong thùng',  'URGENT',     'Mắt sưng, chảy mủ',          2),
  (3, 3, 'RESCUE', 5,  NULL, now() - interval '60 days', 'Đi lạc',             'STABLE',     'Khỏe, chưa tiêm phòng',      2),
  (4, 4, 'RESCUE', 6,  NULL, now() - interval '55 days', 'Mắc kẹt',            'STABLE',     'Trầy xước nhẹ',              2),
  (5, 5, 'RESCUE', 7,  NULL, now() - interval '50 days', 'Bị bỏ rơi',          'NEED_CHECK', 'Vết hằn xích ở cổ',          3),
  (6, 6, 'RESCUE', 8,  NULL, now() - interval '90 days', 'Bị thương',          'NEED_CHECK', 'Rách tai trái',              3),
  (7, 7, 'RESCUE', 9,  NULL, now() - interval '80 days', 'Tai nạn giao thông', 'URGENT',     'Chân trước sưng',            2),
  (8, 8, 'RESCUE', 10, NULL, now() - interval '20 days', 'Suy kiệt',           'URGENT',     'Bỏ ăn, mất nước nặng',       3);

INSERT INTO care_logs (pet_id, staff_id, activity_type, weight_kg, note, logged_at) VALUES
  (1, 2, 'WEIGHT',      6.2,  'Cân lúc nhập trại',               now() - interval '3 days'),
  (1, 2, 'FEEDING',     NULL, 'Ăn hết khẩu phần',                now() - interval '1 day'),
  (2, 3, 'HEALTH_NOTE', NULL, 'Chuyển lưu trú tại phòng khám',   now() - interval '9 days'),
  (3, 2, 'AREA_CHANGE', NULL, 'Hết cách ly, chuyển khu chung',   now() - interval '45 days'),
  (7, 2, 'HEALTH_NOTE', NULL, 'Tiếp nhận lại, hơi sợ người',     now() - interval '2 days');

INSERT INTO vet_requests (id, pet_id, clinic_id, reason, priority, care_mode, status, diagnosis_summary, treatment_summary,
                          total_cost, aftercare_instructions, created_by, requested_at, completed_at, health_ok) VALUES
  (1, 2, 2, 'Nhiễm trùng mắt',               'URGENT',     'INPATIENT',  'IN_TREATMENT', NULL, NULL, NULL, NULL, 2, now() - interval '9 days', NULL, NULL),
  (2, 3, 1, 'Khám tổng quát, tiêm phòng',    'STABLE',     'OUTPATIENT', 'COMPLETED', 'Khỏe mạnh', 'Tiêm 7 bệnh, tẩy giun, triệt sản', 1200000, 'Kiêng tắm 7 ngày', 2, now() - interval '50 days', now() - interval '48 days', TRUE),
  (3, 4, 1, 'Khám tổng quát, tiêm phòng',    'STABLE',     'OUTPATIENT', 'COMPLETED', 'Khỏe mạnh', 'Tiêm 4 bệnh, tẩy giun, triệt sản', 950000, NULL, 2, now() - interval '45 days', now() - interval '44 days', TRUE),
  (4, 5, 2, 'Vết thương cổ, tiêm phòng',     'NEED_CHECK', 'OUTPATIENT', 'COMPLETED', 'Viêm da nhẹ', 'Bôi thuốc, tiêm phòng, triệt sản', 1500000, 'Bôi thuốc ngày 2 lần', 3, now() - interval '45 days', now() - interval '40 days', TRUE),
  (5, 6, 1, 'Khâu tai, tiêm phòng',          'NEED_CHECK', 'OUTPATIENT', 'COMPLETED', 'Rách tai', 'Khâu 3 mũi, tiêm phòng, triệt sản', 1800000, 'Tái khám sau 10 ngày', 3, now() - interval '88 days', now() - interval '85 days', TRUE),
  (6, 7, 2, 'Chụp X-quang chân trước',       'URGENT',     'INPATIENT',  'COMPLETED', 'Nứt xương', 'Bó bột 4 tuần, tiêm phòng, triệt sản', 3500000, 'Hạn chế vận động', 2, now() - interval '79 days', now() - interval '70 days', TRUE),
  (7, 8, 1, 'Suy kiệt, mất nước',            'URGENT',     'INPATIENT',  'COMPLETED', 'Suy thận cấp', 'Truyền dịch', 800000, NULL, 3, now() - interval '20 days', now() - interval '18 days', FALSE);

INSERT INTO adoption_applications (id, pet_id, applicant_id, housing_type, has_pet_experience, motivation,
                                   status, review_note, reviewed_by, reviewed_at, submitted_at) VALUES
  (1, 3, 4, 'HOUSE',     TRUE,  'Nhà có sân rộng',           'PENDING',     NULL, NULL, NULL, now() - interval '2 days'),
  (2, 3, 5, 'APARTMENT', FALSE, 'Muốn có bạn đồng hành',     'PENDING',     NULL, NULL, NULL, now() - interval '1 day'),
  (3, 5, 6, 'HOUSE',     TRUE,  'Từng nuôi chó 10 năm',      'APPROVED',    'Điều kiện tốt', 2, now() - interval '3 days', now() - interval '6 days'),
  (4, 6, 5, 'APARTMENT', TRUE,  'Nhà có một mèo cái',        'HANDED_OVER', 'Phù hợp', 3, now() - interval '35 days', now() - interval '40 days'),
  (5, 6, 6, 'OTHER',     FALSE, 'Thích mèo trắng',           'CANCELLED',   NULL, NULL, NULL, now() - interval '38 days'),
  (6, 7, 4, 'HOUSE',     TRUE,  'Muốn nuôi chó giữ nhà',     'HANDED_OVER', 'Phù hợp', 2, now() - interval '62 days', now() - interval '65 days'),
  (7, 4, 5, 'APARTMENT', FALSE, 'Muốn nuôi mèo',             'REJECTED',    'Chung cư không cho nuôi thú', 2, now() - interval '10 days', now() - interval '12 days');
UPDATE adoption_applications SET cancel_reason = 'Hệ thống tự đóng: thú đã có đơn khác được duyệt' WHERE id = 5;

UPDATE adoption_applications SET handover_date = current_date + 2 WHERE id = 3;

INSERT INTO handovers (id, application_id, pet_id, staff_id, handed_over_at, donation_amount, note, follow_up_status) VALUES
  (1, 4, 6, 3, now() - interval '30 days', 500000, 'Bàn giao tại trung tâm', 'IN_PROGRESS'),
  (2, 6, 7, 2, now() - interval '60 days', 0,      NULL,                     'RETURNED');

INSERT INTO follow_ups (handover_id, sequence_no, is_additional, due_date, status, result, contact_method,
                        health_ok, housing_ok, commitment_ok, pet_present, owner_reachable, reason, performed_by, performed_at) VALUES
  (1, 1, FALSE, current_date - 16, 'DONE',      'PASS',  'PHONE', TRUE, TRUE, TRUE, TRUE, TRUE, NULL, 3, now() - interval '16 days'),
  (1, 2, FALSE, current_date + 30, 'SCHEDULED', NULL,    NULL,    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
  (2, 1, FALSE, current_date - 46, 'DONE',      'WATCH', 'PHONE', TRUE, FALSE, TRUE, TRUE, TRUE, 'Sân chưa có rào, chó hay chạy ra đường', 2, now() - interval '46 days'),
  (2, 2, FALSE, current_date,      'CANCELLED', NULL,    NULL,    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO intakes (id, pet_id, source, rescue_case_id, returned_handover_id, intake_at, reason, priority, initial_assessment, created_by) VALUES
  (9, 7, 'RETURN', NULL, 2, now() - interval '2 days', 'Chủ nuôi chuyển nhà, không nuôi tiếp', 'STABLE', 'Khỏe, hơi sợ người', 2);

INSERT INTO pet_end_events (pet_id, event_type, occurred_at, detail, recorded_by) VALUES
  (8, 'DECEASED', now() - interval '17 days', 'Suy thận cấp, không qua khỏi', 3);

INSERT INTO status_histories (entity_type, entity_id, from_status, to_status, event, changed_by, changed_at) VALUES
  ('PET', 1, NULL,               'IN_CARE',          'RESCUE_SUCCEEDED',    2, now() - interval '3 days'),
  ('PET', 2, NULL,               'IN_CARE',          'RESCUE_SUCCEEDED',    2, now() - interval '10 days'),
  ('PET', 2, 'IN_CARE',          'IN_TREATMENT',     'VET_REQUEST_OPENED',  2, now() - interval '9 days'),
  ('PET', 3, NULL,               'IN_CARE',          'RESCUE_SUCCEEDED',    2, now() - interval '60 days'),
  ('PET', 3, 'IN_CARE',          'IN_TREATMENT',     'VET_REQUEST_OPENED',  2, now() - interval '50 days'),
  ('PET', 3, 'IN_TREATMENT',     'IN_CARE',          'VET_REQUEST_CLOSED',  2, now() - interval '48 days'),
  ('PET', 3, 'IN_CARE',          'AVAILABLE',        'ELIGIBILITY_PASSED',  2, now() - interval '20 days'),
  ('PET', 4, NULL,               'IN_CARE',          'RESCUE_SUCCEEDED',    2, now() - interval '55 days'),
  ('PET', 4, 'IN_CARE',          'IN_TREATMENT',     'VET_REQUEST_OPENED',  2, now() - interval '45 days'),
  ('PET', 4, 'IN_TREATMENT',     'IN_CARE',          'VET_REQUEST_CLOSED',  2, now() - interval '44 days'),
  ('PET', 4, 'IN_CARE',          'AVAILABLE',        'ELIGIBILITY_PASSED',  2, now() - interval '25 days'),
  ('PET', 5, NULL,               'IN_CARE',          'RESCUE_SUCCEEDED',    3, now() - interval '50 days'),
  ('PET', 5, 'IN_CARE',          'IN_TREATMENT',     'VET_REQUEST_OPENED',  3, now() - interval '45 days'),
  ('PET', 5, 'IN_TREATMENT',     'IN_CARE',          'VET_REQUEST_CLOSED',  3, now() - interval '40 days'),
  ('PET', 5, 'IN_CARE',          'AVAILABLE',        'ELIGIBILITY_PASSED',  3, now() - interval '30 days'),
  ('PET', 5, 'AVAILABLE',        'PENDING_HANDOVER', 'APPLICATION_APPROVED', 2, now() - interval '3 days'),
  ('PET', 6, NULL,               'IN_CARE',          'RESCUE_SUCCEEDED',    3, now() - interval '90 days'),
  ('PET', 6, 'IN_CARE',          'IN_TREATMENT',     'VET_REQUEST_OPENED',  3, now() - interval '88 days'),
  ('PET', 6, 'IN_TREATMENT',     'IN_CARE',          'VET_REQUEST_CLOSED',  3, now() - interval '85 days'),
  ('PET', 6, 'IN_CARE',          'AVAILABLE',        'ELIGIBILITY_PASSED',  3, now() - interval '60 days'),
  ('PET', 6, 'AVAILABLE',        'PENDING_HANDOVER', 'APPLICATION_APPROVED', 3, now() - interval '35 days'),
  ('PET', 6, 'PENDING_HANDOVER', 'ADOPTED',          'HANDED_OVER',         3, now() - interval '30 days'),
  ('PET', 7, NULL,               'IN_CARE',          'RESCUE_SUCCEEDED',    2, now() - interval '80 days'),
  ('PET', 7, 'IN_CARE',          'IN_TREATMENT',     'VET_REQUEST_OPENED',  2, now() - interval '79 days'),
  ('PET', 7, 'IN_TREATMENT',     'IN_CARE',          'VET_REQUEST_CLOSED',  2, now() - interval '70 days'),
  ('PET', 7, 'IN_CARE',          'AVAILABLE',        'ELIGIBILITY_PASSED',  2, now() - interval '66 days'),
  ('PET', 7, 'AVAILABLE',        'PENDING_HANDOVER', 'APPLICATION_APPROVED', 2, now() - interval '62 days'),
  ('PET', 7, 'PENDING_HANDOVER', 'ADOPTED',          'HANDED_OVER',         2, now() - interval '60 days'),
  ('PET', 7, 'ADOPTED',          'IN_CARE',          'RETURNED',            2, now() - interval '2 days'),
  ('PET', 8, NULL,               'IN_CARE',          'RESCUE_SUCCEEDED',    3, now() - interval '20 days'),
  ('PET', 8, 'IN_CARE',          'IN_TREATMENT',     'VET_REQUEST_OPENED',  3, now() - interval '19 days'),
  ('PET', 8, 'IN_TREATMENT',     'IN_CARE',          'VET_REQUEST_CLOSED',  3, now() - interval '18 days'),
  ('PET', 8, 'IN_CARE',          'DECEASED',         'DECEASED',            3, now() - interval '17 days'),
  ('HANDOVER', 1, NULL,          'IN_PROGRESS',      'HANDED_OVER',         3, now() - interval '30 days'),
  ('HANDOVER', 2, NULL,          'IN_PROGRESS',      'HANDED_OVER',         2, now() - interval '60 days'),
  ('HANDOVER', 2, 'IN_PROGRESS', 'RETURNED',         'RETURNED',            2, now() - interval '2 days');

INSERT INTO rescue_reports (id, code, reporter_name, reporter_phone, location_text, description, status, handled_by, created_at, handled_at) VALUES
  (13, 'BC-K7P3DS', 'Ngọc Hân',   '0911000013', 'Ngã tư Hàng Xanh, Bình Thạnh',   'Chó nhỏ chạy giữa đường, có vẻ hoảng sợ',          'PROCESSED', 2, now() - interval '4 hours',  now() - interval '4 hours'),
  (14, 'BC-N2V6QE', 'Tuấn Kiệt',  '0911000014', 'Đường Nguyễn Văn Linh, Quận 7',  'Mèo xám bị bỏ trước cửa hàng',                     'PROCESSED', 3, now() - interval '180 days', now() - interval '180 days'),
  (15, 'BC-P8L4WZ', 'Hồng Nhung', '0911000015', 'Phố đi bộ Nguyễn Huệ, Quận 1',   'Chó xám đeo vòng cổ đỏ đi lạc',                    'PROCESSED', 2, now() - interval '40 days',  now() - interval '40 days'),
  (16, 'BC-S5D9TM', 'Phúc Thịnh', '0911000016', 'Bến Bạch Đằng, Quận 1',          'Chó lông xám đeo vòng cổ đỏ, quen người, đi lang thang', 'PROCESSED', 2, now() - interval '1 day', now() - interval '1 day');

INSERT INTO rescue_cases (id, location_text, assigned_staff_id, status, failure_reason, created_by, created_at, closed_at) VALUES
  (11, 'Ngã tư Hàng Xanh, Bình Thạnh',  NULL, 'NEW',         NULL, 2, now() - interval '4 hours',  NULL),
  (12, 'Đường Nguyễn Văn Linh, Quận 7', 3,    'SUCCEEDED',   NULL, 3, now() - interval '180 days', now() - interval '180 days'),
  (13, 'Phố đi bộ Nguyễn Huệ, Quận 1',  2,    'SUCCEEDED',   NULL, 2, now() - interval '40 days',  now() - interval '40 days'),
  (14, 'Bến Bạch Đằng, Quận 1',         2,    'IN_PROGRESS', NULL, 2, now() - interval '1 day',    NULL);

INSERT INTO report_cases (report_id, case_id) VALUES (13, 11), (14, 12), (15, 13), (16, 14);

INSERT INTO pets (id, code, name, species, sex, estimated_birth_date, color, description, status, area, current_location,
                  is_vaccinated, is_dewormed, is_sterilized, temperament_ok, eligibility_checked_at, eligibility_checked_by, created_at) VALUES
  (9,  'TH-2026-0009', 'Na', 'CAT', 'FEMALE', current_date - 800, 'Xám', 'Đã nhận nuôi, hậu kiểm lần 2 không đạt, đang chờ thu hồi', 'ADOPTED', NULL, 'Nhà chủ nuôi', TRUE, TRUE, TRUE, TRUE, now() - interval '130 days', 2, now() - interval '180 days'),
  (10, 'TH-2026-0010', 'Ki', 'DOG', 'MALE',   current_date - 450, 'Xám, vòng cổ đỏ', 'Sổng chuồng khi đưa đi khám, đang thất lạc', 'LOST', NULL, NULL, TRUE, TRUE, FALSE, TRUE, NULL, NULL, now() - interval '40 days');

INSERT INTO intakes (id, pet_id, source, rescue_case_id, returned_handover_id, intake_at, reason, priority, initial_assessment, created_by) VALUES
  (10, 9,  'RESCUE', 12, NULL, now() - interval '180 days', 'Bị bỏ rơi', 'STABLE',     'Khỏe, hơi gầy',               3),
  (11, 10, 'RESCUE', 13, NULL, now() - interval '40 days',  'Đi lạc',    'NEED_CHECK', 'Có vòng cổ đỏ, không rõ chủ', 2);

INSERT INTO vet_requests (id, pet_id, clinic_id, reason, priority, care_mode, status, diagnosis_summary, treatment_summary,
                          total_cost, aftercare_instructions, created_by, requested_at, completed_at, health_ok) VALUES
  (8, 9,  1, 'Khám tổng quát, tiêm phòng', 'STABLE',     'OUTPATIENT', 'COMPLETED', 'Khỏe mạnh', 'Tiêm 4 bệnh, tẩy giun, triệt sản', 900000, NULL, 3, now() - interval '175 days', now() - interval '174 days', TRUE),
  (9, 10, 1, 'Khám tổng quát, tiêm phòng', 'NEED_CHECK', 'OUTPATIENT', 'COMPLETED', 'Khỏe mạnh', 'Tiêm 7 bệnh, tẩy giun',           600000, NULL, 2, now() - interval '38 days', now() - interval '37 days', TRUE);

INSERT INTO adoption_applications (id, pet_id, applicant_id, housing_type, has_pet_experience, motivation,
                                   status, review_note, reviewed_by, reviewed_at, submitted_at) VALUES
  (8, 9, 5, 'APARTMENT', TRUE, 'Muốn có thêm bạn cho Miu', 'HANDED_OVER', 'Phù hợp', 3, now() - interval '105 days', now() - interval '110 days');

INSERT INTO handovers (id, application_id, pet_id, staff_id, handed_over_at, donation_amount, note, follow_up_status) VALUES
  (3, 8, 9, 3, now() - interval '100 days', 200000, 'Bàn giao tại trung tâm', 'RECALL_PENDING');

INSERT INTO follow_ups (handover_id, sequence_no, is_additional, due_date, status, result, contact_method,
                        health_ok, housing_ok, commitment_ok, pet_present, owner_reachable, reason, performed_by, performed_at) VALUES
  (3, 1, FALSE, current_date - 86, 'DONE', 'PASS', 'PHONE',      TRUE,  TRUE,  TRUE, TRUE,  TRUE, NULL, 3, now() - interval '85 days'),
  (3, 2, FALSE, current_date - 40, 'DONE', 'FAIL', 'HOME_VISIT', FALSE, FALSE, TRUE, FALSE, TRUE,
   'Chủ đã cho mèo sang nhà người quen, không còn ở với chủ', 3, now() - interval '35 days');

INSERT INTO pet_end_events (pet_id, event_type, occurred_at, detail, recorded_by) VALUES
  (10, 'LOST', now() - interval '5 days', 'Sổng chuồng khi đưa đi dạo ở Bến Bạch Đằng', 2);

INSERT INTO status_histories (entity_type, entity_id, from_status, to_status, event, note, changed_by, changed_at) VALUES
  ('PET', 9, NULL,               'IN_CARE',          'RESCUE_SUCCEEDED',     NULL, 3, now() - interval '180 days'),
  ('PET', 9, 'IN_CARE',          'IN_TREATMENT',     'VET_REQUEST_OPENED',   NULL, 3, now() - interval '175 days'),
  ('PET', 9, 'IN_TREATMENT',     'IN_CARE',          'VET_REQUEST_CLOSED',   NULL, 3, now() - interval '174 days'),
  ('PET', 9, 'IN_CARE',          'AVAILABLE',        'ELIGIBILITY_PASSED',   NULL, 2, now() - interval '130 days'),
  ('PET', 9, 'AVAILABLE',        'PENDING_HANDOVER', 'APPLICATION_APPROVED', 'Đơn #8', 3, now() - interval '105 days'),
  ('PET', 9, 'PENDING_HANDOVER', 'ADOPTED',          'HANDED_OVER',          'Đơn #8', 3, now() - interval '100 days'),
  ('HANDOVER', 3, NULL,          'IN_PROGRESS',      'HANDED_OVER',          NULL, 3, now() - interval '100 days'),
  ('HANDOVER', 3, 'IN_PROGRESS', 'RECALL_PENDING',   'FOLLOW_UP_FAILED',     'Hậu kiểm lần 2: chủ đã cho mèo sang nhà người quen', 3, now() - interval '35 days'),
  ('PET', 10, NULL,              'IN_CARE',          'RESCUE_SUCCEEDED',     NULL, 2, now() - interval '40 days'),
  ('PET', 10, 'IN_CARE',         'IN_TREATMENT',     'VET_REQUEST_OPENED',   NULL, 2, now() - interval '38 days'),
  ('PET', 10, 'IN_TREATMENT',    'IN_CARE',          'VET_REQUEST_CLOSED',   NULL, 2, now() - interval '37 days'),
  ('PET', 10, 'IN_CARE',         'LOST',             'LOST',                 'Thất lạc: sổng chuồng khi đưa đi dạo', 2, now() - interval '5 days');

PERFORM setval('users_id_seq',                 (SELECT max(id) FROM users));
PERFORM setval('vet_clinics_id_seq',           (SELECT max(id) FROM vet_clinics));
PERFORM setval('rescue_reports_id_seq',        (SELECT max(id) FROM rescue_reports));
PERFORM setval('rescue_cases_id_seq',          (SELECT max(id) FROM rescue_cases));
PERFORM setval('pets_id_seq',                  (SELECT max(id) FROM pets));
PERFORM setval('intakes_id_seq',               (SELECT max(id) FROM intakes));
PERFORM setval('vet_requests_id_seq',          (SELECT max(id) FROM vet_requests));
PERFORM setval('adoption_applications_id_seq', (SELECT max(id) FROM adoption_applications));
PERFORM setval('handovers_id_seq',             (SELECT max(id) FROM handovers));
PERFORM setval('pet_code_seq',                 10);
END
$seed$;
