# Animal Aid and Rescue

Hệ thống quản lý cứu hộ thú cưng và nhận nuôi, đồ án môn Phân tích thiết kế hệ thống thông tin nâng cao (nhóm 3 người).

Hệ thống theo dõi một con thú (chó hoặc mèo) suốt vòng đời tại một trung tâm cứu hộ: báo cứu hộ, chăm sóc và phối hợp thú y, nhận nuôi, bàn giao, hậu kiểm, trả lại và tái tiếp nhận. Cơ sở thú y là thực thể ngoài, nhân viên nhập hộ kết quả điều trị.

## Tài liệu

| Tài liệu | Nội dung |
| --- | --- |
| [docs/report.md](docs/report.md) | Báo cáo đồ án (bản xuất từ tài liệu online). Chương 2 đã chốt. Chương 4 và 5 có đủ phần tối thiểu để code |
| [docs/db/V1__init.sql](docs/db/V1__init.sql) | Schema PostgreSQL 16 bảng, dùng làm migration Flyway V1 |
| [Bản online](https://claude.ai/artifact/UESS64WHDvFkrjWkQciXVM) | Bản gốc để review và comment, có đầy đủ sơ đồ |

Bản online là nguồn chính. Khi bản online thay đổi, xuất lại `docs/report.md`.

Mục cần đọc trước khi code:

- 2.3 Quy tắc nghiệp vụ (QĐ-01 đến QĐ-16)
- 3.4 Bảng chuyển trạng thái (Thú cưng, Đơn nhận nuôi, Yêu cầu thú y, Bàn giao, Báo cáo, Ca)
- 4.2 Mô hình quan hệ và tham số hệ thống
- 4.4 Ràng buộc toàn vẹn (RB-01 đến RB-15) và xử lý đồng thời khi duyệt đơn
- 5.1 Kiến trúc, quy ước API, lộ trình MVP
- 5.2 Danh sách màn hình và ma trận phân quyền

## Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Backend | Spring Boot, Spring Security (JWT), Spring Data JPA |
| Frontend | Next.js, SCSS thuần theo chuẩn BEM (chỉ làm giao diện, mọi dữ liệu đi qua API Spring) |
| Cơ sở dữ liệu | PostgreSQL |
| Migration | Flyway |
| Hợp đồng API | springdoc-openapi, sinh client TypeScript cho frontend |
| Môi trường | Docker Compose |
| Kiểm thử | JUnit, Testcontainers |

## Quy ước

- **API:** `/api/public/**` (Khách), `/api/adopter/**` (Người nhận nuôi), `/api/staff/**` (Nhân viên), `/api/admin/**` (Quản lý).
- **Trạng thái:** chỉ đổi qua một service duy nhất đọc bảng chuyển trạng thái ở mục 3.4, mỗi lần đổi ghi `status_histories`. Không cập nhật cột `status` trực tiếp ở nơi khác.
- **Database:** tên bảng và cột tiếng Anh, snake_case. Migration Flyway đánh số tăng dần, không sửa migration đã gộp vào nhánh chính. Không xóa cứng hồ sơ thú, đơn, bàn giao, lượt tiếp nhận.
- **SCSS:** mỗi component một file `.scss`, class theo BEM `block__element--modifier`, biến màu và khoảng cách đặt chung một thư mục.
- **Git:** nhánh theo tính năng, review chéo trước khi gộp.

## Lộ trình

| Đợt | Phạm vi |
| --- | --- |
| MVP | Báo cứu hộ, xử lý báo cáo và ca, hồ sơ thú và lượt tiếp nhận, nhật ký chăm sóc, yêu cầu thú y, checklist, nộp và duyệt đơn có xử lý đồng thời, bàn giao, hậu kiểm, trả lại, đăng nhập và phân quyền, quản lý tài khoản, cơ sở thú y, tham số |
| Đợt 2 | Gộp báo cáo trùng, tra cứu báo cáo bằng mã, sự kiện kết thúc và tìm lại thú thất lạc, khoản ủng hộ và 3 con số chi phí, báo cáo thống kê |

## Việc cần chốt trong sprint 0

- [ ] Quyền của Quản lý với các chức năng nghiệp vụ (hiện tài liệu ghi "chỉ xem")
- [ ] Danh sách API cho các màn hình MVP
- [ ] Định dạng mã báo cáo và mã hồ sơ thú
- [ ] Dữ liệu mẫu cho demo
