# Animal Aid and Rescue

Hệ thống quản lý cứu hộ thú cưng và nhận nuôi, đồ án môn Phân tích thiết kế hệ thống thông tin nâng cao (nhóm 3 người).

Hệ thống theo dõi một con thú (chó hoặc mèo) suốt vòng đời tại một trung tâm cứu hộ: báo cứu hộ, chăm sóc và phối hợp thú y, nhận nuôi, bàn giao, hậu kiểm, trả lại và tái tiếp nhận. Cơ sở thú y là thực thể ngoài, nhân viên nhập hộ kết quả điều trị.

## Cấu trúc repo

```
backend/    Spring Boot (Java 21), Flyway, JWT
frontend/   Next.js, SCSS thuần theo BEM
docs/       Báo cáo đồ án (bản xuất) và schema
docker-compose.yml   PostgreSQL cho môi trường dev
```

## Chạy dự án

Cần cài: JDK 21, Maven 3.9+, Node.js 22+, Docker Desktop.

```bash
# 1. Cơ sở dữ liệu
docker compose up -d

# 2. Backend (profile dev nạp dữ liệu mẫu), chạy ở http://localhost:8080
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Frontend, chạy ở http://localhost:3000
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

- Tài liệu API (Swagger): http://localhost:8080/swagger-ui.html
- Kiểm thử backend: `mvn test`. Test tích hợp chạy trên PostgreSQL thật qua Testcontainers, cần Docker đang chạy (không có Docker thì test này tự bỏ qua).

**Tài khoản mẫu** (mật khẩu chung `Password@123`):

| Email | Vai trò |
| --- | --- |
| manager@aar.local | Quản lý |
| staff1@aar.local, staff2@aar.local | Nhân viên |
| adopter1@aar.local, adopter2@aar.local, adopter3@aar.local | Người nhận nuôi |

## Tài liệu

| Tài liệu | Nội dung |
| --- | --- |
| [docs/report.md](docs/report.md) | Báo cáo đồ án (bản xuất từ tài liệu online) |
| [Bản online](https://claude.ai/artifact/UESS64WHDvFkrjWkQciXVM) | Bản gốc để review và comment, có đầy đủ sơ đồ |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Quy ước Git, backend, frontend, định nghĩa hoàn thành |

Bản online là nguồn chính. Khi bản online thay đổi, xuất lại `docs/report.md`. Schema chính thức là migration trong `backend/src/main/resources/db/migration`; `docs/db/V1__init.sql` chỉ là bản sao để đọc kèm báo cáo.

Mục cần đọc trước khi code:

- 2.3 Quy tắc nghiệp vụ (QĐ-01 đến QĐ-16)
- 3.4 Bảng chuyển trạng thái, đã cài trong `backend/.../common/state/TransitionTable.java`
- 4.2 Mô hình quan hệ, tham số hệ thống, quy tắc sinh mã
- 4.4 Ràng buộc toàn vẹn (RB-01 đến RB-15) và xử lý đồng thời khi duyệt đơn
- 5.1 Kiến trúc, quy ước API, lộ trình MVP
- 5.2 Danh sách màn hình, ma trận phân quyền và danh sách API cho MVP
- 5.4 Dữ liệu mẫu cho demo

## Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Backend | Spring Boot 3.5, Spring Security (JWT, RoleHierarchy MANAGER > STAFF), Spring Data JPA |
| Frontend | Next.js 16, SCSS thuần theo chuẩn BEM (chỉ làm giao diện, mọi dữ liệu đi qua API Spring) |
| Cơ sở dữ liệu | PostgreSQL 16 |
| Migration | Flyway |
| Hợp đồng API | springdoc-openapi |
| Môi trường | Docker Compose |
| Kiểm thử | JUnit, Testcontainers |

## Đã có sau sprint 0

- Schema 16 bảng (migration V1), sequence sinh mã hồ sơ thú (V2), dữ liệu mẫu đủ mọi trạng thái.
- Bảo mật: đăng ký, đăng nhập JWT, phân quyền theo đường dẫn, Quản lý kế thừa quyền Nhân viên.
- `TransitionTable` và `StateTransitionService`: toàn bộ bảng chuyển trạng thái mục 3.4, ghi `status_histories`.
- API mẫu: `GET /api/public/pets`, `GET /api/public/pets/{id}`, `POST /api/public/auth/register|login`, `GET /api/me`, `GET /api/staff/dashboard`.
- Định dạng lỗi thống nhất (`ApiError`), lỗi trạng thái và xung đột đồng thời trả 409.
- Frontend: trang danh sách thú sẵn sàng nhận nuôi (P01), trang đăng nhập (P05), bộ SCSS dùng chung.

## Đã có sau sprint 1 (MVP backend)

Toàn bộ API MVP ở báo cáo mục 5.2, mỗi thao tác đổi trạng thái đi qua `StateTransitionService`, có test tích hợp trên PostgreSQL thật (115 test, `mvn test`).

| Nhóm | Package | API chính |
| --- | --- | --- |
| Cứu hộ | `rescue` | Báo cáo (mã BC-, ảnh, giới hạn 5 báo cáo/giờ/số điện thoại), tạo ca tách nhiều thú, không hợp lệ, phân công, ca thành công tạo thú + lượt tiếp nhận, ca thất bại |
| Chăm sóc, thú y | `care` | Hồ sơ thú, nhật ký, yêu cầu thú y (tạo, bắt đầu, hoàn tất, hủy), checklist 6 mục, không đủ điều kiện, lịch sử trạng thái |
| Nhận nuôi | `adoption` | Nộp, hủy đơn, duyệt có khóa dòng thú (`SELECT ... FOR UPDATE`), từ chối |
| Bàn giao | `handover` | Bàn giao + 2 lịch hậu kiểm, kết quả hậu kiểm (Đạt / Cần theo dõi / Không đạt), tiếp nhận trả lại, thú đã nhận của Người nhận nuôi |
| Quản trị | `system` | Tài khoản, cơ sở thú y, tham số hệ thống |
| Tệp đính kèm | `common.storage` | Ảnh báo cáo, ảnh thú, phiếu khám, ảnh hậu kiểm. Lưu ở `STORAGE_DIR` (mặc định `backend/data/uploads`, đã gitignore) |

Ghi chú khi nâng cấp database đã có: migration `V3` thêm tham số `MIN_STERILIZATION_AGE_WEEKS`, Flyway tự chạy khi khởi động.

## Lộ trình

| Đợt | Phạm vi |
| --- | --- |
| MVP | Báo cứu hộ, xử lý báo cáo và ca, hồ sơ thú và lượt tiếp nhận, nhật ký chăm sóc, yêu cầu thú y, checklist, nộp và duyệt đơn có xử lý đồng thời, bàn giao, hậu kiểm, trả lại, quản lý tài khoản, cơ sở thú y, tham số |
| Đợt 2 | Gộp báo cáo trùng, tra cứu báo cáo bằng mã, sự kiện kết thúc và tìm lại thú thất lạc, khoản ủng hộ và 3 con số chi phí, báo cáo thống kê |
