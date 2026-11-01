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

## Lộ trình

| Đợt | Phạm vi |
| --- | --- |
| MVP | Báo cứu hộ, xử lý báo cáo và ca, hồ sơ thú và lượt tiếp nhận, nhật ký chăm sóc, yêu cầu thú y, checklist, nộp và duyệt đơn có xử lý đồng thời, bàn giao, hậu kiểm, trả lại, quản lý tài khoản, cơ sở thú y, tham số |
| Đợt 2 | Gộp báo cáo trùng, tra cứu báo cáo bằng mã, sự kiện kết thúc và tìm lại thú thất lạc, khoản ủng hộ và 3 con số chi phí, báo cáo thống kê |
