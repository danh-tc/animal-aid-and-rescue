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

## Đã có sau sprint 2 (MVP frontend)

Giao diện cho toàn bộ màn hình MVP ở báo cáo mục 5.3, gọi API thật qua `lib/api.ts`. Hệ thiết kế chọn theo [ui-ux-pro-max](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill) cho nhóm "pet adoption, nonprofit": xanh ngọc tạo tin cậy, cam ấm chỉ dùng cho lời kêu gọi hành động, font Nunito / Nunito Sans (có bộ chữ tiếng Việt), icon SVG lucide. Vẫn là SCSS thuần theo BEM, không Tailwind.

| Nhóm | Đường dẫn | Màn hình |
| --- | --- | --- |
| Công khai | `/`, `/pets/[id]`, `/rescue`, `/login`, `/register` | P01, P02, P03, P05 |
| Người nhận nuôi | `/pets/[id]/apply`, `/me/applications`, `/me/adoptions` | A01, A02, A03 |
| Nhân viên | `/staff`, `/staff/reports`, `/staff/cases`, `/staff/pets`, `/staff/vet-requests`, `/staff/applications`, `/staff/follow-ups` | S01 đến S10 (S08 bàn giao trong chi tiết đơn, S10 nhận lại trong tab Bàn giao của hồ sơ thú) |
| Quản lý | `/admin/users`, `/admin/clinics`, `/admin/parameters` | M02, M03, M04 |

- **Bảo vệ route theo vai trò** hai lớp: `src/proxy.ts` chuyển hướng sớm theo cookie `aar_role`, `RequireRole` kiểm tra lại phía client. Quyền thật vẫn do backend kiểm tra JWT.
- **Giao diện sáng và tối** theo cài đặt hệ điều hành, token màu nằm ở `styles/base/_tokens.scss`, các cặp chữ/nền đạt WCAG AA.
- **Không cuộn ngang ở 375px**: bảng tự chuyển thành thẻ trên điện thoại, sidebar nhân viên thành ngăn kéo dưới 1024px.
- **Trợ năng**: liên kết bỏ qua điều hướng, nhãn cho mọi ô nhập, lỗi gắn `aria-describedby`, hộp thoại `<dialog>` gốc, tab dùng phím mũi tên, tôn trọng `prefers-reduced-motion`.

Đã chạy thử toàn bộ luồng trên dữ liệu mẫu bằng Playwright: báo cứu hộ kèm ảnh, tạo 2 ca, cứu thành công, tạo và hoàn tất yêu cầu thú y, checklist, đăng ký và nộp đơn, duyệt, bàn giao, hậu kiểm Cần theo dõi (tự thêm lần bổ sung), nhận lại thú, sửa tham số, tạo nhân viên. Không có lỗi console.

## Đã có sau Đợt 2

Toàn bộ phạm vi Đợt 2 ở báo cáo mục 5.1, cùng các chỗ trước đây code còn lệch so với `BaoCao_DoAn.docx`.

| Hạng mục | Báo cáo | Đường dẫn |
| --- | --- | --- |
| Tra cứu báo cáo bằng mã và số điện thoại | FR-02, P04, RB-15 | `/rescue/lookup`, `GET /api/public/rescue-reports/lookup` |
| Ca đang mở ở gần (sắp theo khoảng cách) và gộp báo cáo trùng | QĐ-01, UC-07 bước 2 và 2a | S02, `POST /api/staff/rescue-reports/{id}/merge` |
| Sự kiện kết thúc: qua đời, thất lạc, chuyển cơ sở | FR-16, QĐ-15, QĐ-16, bảng 3.4 | S05/S10, `POST /api/staff/pets/{id}/end-events` |
| Tìm lại thú thất lạc, lượt tiếp nhận FOUND | QĐ-16 | `POST /api/staff/pets/{id}/found` |
| 3 con số chi phí khi bàn giao | QĐ-14, UC-11 bước 2 | S08, hồ sơ thú, `GET /api/staff/pets/{id}/costs` |
| Báo cáo thống kê: tổng hợp, ngoại lệ, chi phí và ủng hộ | FR-18, M01, mục 5.3 | `/admin/reports`, `GET /api/admin/reports/{summary,exceptions,costs}` |
| Ghi kết quả thú y, hậu kiểm dạng multipart kèm tệp | mục 5.2 | phần `data` là JSON, phần `files` (thú y) hoặc `photos` (hậu kiểm). Vẫn nhận JSON thuần |
| Giới hạn tần suất `/api/public/**` theo IP | mục 5.1 | đăng nhập/đăng ký 10, tra cứu 20, gửi báo cáo 10, còn lại 300 yêu cầu mỗi phút (`app.rate-limit.*`) |
| Sinh client TypeScript từ springdoc | mục 5.1 | `npm run gen:api` (backend đang chạy) => `src/types/openapi.ts`, `src/types/contract.ts` báo lỗi biên dịch khi API đổi |
| Sao lưu hằng ngày và khôi phục | NFR-06 | xem bên dưới |

Test backend: 139 test (`mvn test`). Đã chạy thử toàn bộ luồng MVP và Đợt 2 bằng Playwright trên dữ liệu mẫu.

### Sao lưu và khôi phục (NFR-06)

```bash
docker compose up -d            # gồm cả container backup: sao lưu lúc khởi động và 02:00 mỗi ngày, giữ 7 ngày, lưu ở backups/
./scripts/backup.sh             # sao lưu ngay
./scripts/restore.sh backups/aar-YYYYMMDD-HHMMSS.dump   # khôi phục, hỏi xác nhận trước khi ghi đè
```

### Đăng nhập nhanh bằng Google

1. Google Cloud Console, mục APIs & Services, Credentials: tạo OAuth client ID loại **Web application**.
2. Authorized JavaScript origins thêm `http://localhost:3000` (và tên miền thật khi triển khai).
3. Chạy backend với biến môi trường `GOOGLE_CLIENT_ID=<client id>`. Frontend tự đọc qua `GET /api/public/auth/config`, không cần cấu hình thêm.

Chưa đặt biến này thì nút Google ẩn đi, đăng nhập bằng mật khẩu vẫn dùng bình thường. Email Google đã có tài khoản thì đăng nhập vào tài khoản đó (kể cả nhân viên), chưa có thì tạo tài khoản Người nhận nuôi.

### Phân quyền phân công ca cứu hộ

Chỉ Quản lý được phân công người đi cứu (báo cáo mục 5.2, ma trận phân quyền). Nhân viên tạo ca ở trạng thái Chưa phân công. Gọi API phân công bằng tài khoản Nhân viên sẽ nhận lỗi 403 `ASSIGN_MANAGER_ONLY`.

## Lộ trình

| Đợt | Phạm vi |
| --- | --- |
| MVP | Báo cứu hộ, xử lý báo cáo và ca, hồ sơ thú và lượt tiếp nhận, nhật ký chăm sóc, yêu cầu thú y, checklist, nộp và duyệt đơn có xử lý đồng thời, bàn giao, hậu kiểm, trả lại, quản lý tài khoản, cơ sở thú y, tham số |
| Đợt 2 | Gộp báo cáo trùng, tra cứu báo cáo bằng mã, sự kiện kết thúc và tìm lại thú thất lạc, khoản ủng hộ và 3 con số chi phí, báo cáo thống kê |
