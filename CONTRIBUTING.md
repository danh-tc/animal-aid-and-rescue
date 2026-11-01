# Quy ước làm việc

## Git

- Nhánh chính: `main`, luôn build được.
- Mỗi tính năng một nhánh, đặt tên theo màn hình hoặc yêu cầu: `feature/S07-xet-duyet-don`, `fix/login-error-message`.
- Mở pull request vào `main`, cần ít nhất một thành viên khác review trước khi gộp.
- Commit message ngắn, mô tả việc đã làm: `Thêm API duyệt đơn có khóa dòng thú`.

## Backend

- Package chia theo 6 nhóm chức năng của BFD (báo cáo mục 2.4): `rescue`, `care`, `adoption`, `handover`, `report`, `system`. Phần dùng chung nằm trong `common` và `pet`.
- **Đổi trạng thái chỉ qua `StateTransitionService`** (RB-12). Thêm chuyển trạng thái mới thì sửa `TransitionTable` và báo cáo mục 3.4 cùng lúc.
- Ghi dữ liệu đi kèm (lý do, chi phí, nhận xét) vào entity **trước** khi gọi chuyển trạng thái. CHECK constraint trong database đòi trạng thái mới phải có các dữ liệu này, nếu có flush xen giữa sẽ bị chặn.
- Thao tác thay đổi trạng thái thú phải khóa dòng thú trước (`PetLifecycle.lock`) để tuần tự hóa với các thao tác khác trên cùng thú.
- Test tích hợp kế thừa `support/IntegrationTestBase`, tự tạo dữ liệu qua API (`availablePet()`, `submitApplication()`...) thay vì sửa dữ liệu mẫu.
- Thao tác đổi trạng thái dùng endpoint dạng lệnh (`POST /api/staff/applications/{id}/approve`). Lỗi trạng thái trả 409.
- Lỗi nghiệp vụ ném `BusinessException`, không tự trả `ResponseEntity` lỗi trong controller.
- Migration Flyway đặt ở `src/main/resources/db/migration`, đánh số `V3__mo_ta.sql`, `V4__...`. **Không sửa migration đã gộp vào `main`**, muốn đổi thì viết migration mới.
- Dữ liệu mẫu nằm ở `db/seed/R__dev_seed.sql`, chỉ chạy với profile `dev`.
- Không xóa cứng hồ sơ thú, đơn, bàn giao, lượt tiếp nhận (RB-11).

## Frontend

- SCSS thuần theo chuẩn BEM, không dùng CSS Modules hay thư viện CSS.
- Mỗi component một thư mục: `components/pet-card/PetCard.tsx` và `pet-card.scss`, component tự import file SCSS của mình.
- Tên class: `block`, `block__element`, `block--modifier`, `block__element--modifier`. Một file SCSS chỉ chứa một block.
- Màu, khoảng cách, bo góc lấy từ `styles/abstracts` (`@use "abstracts" as *;`), không viết mã màu trực tiếp.
- Gọi API qua `lib/api.ts`. Mọi dữ liệu và logic nghiệp vụ nằm ở backend, Next.js chỉ làm giao diện.

## Định nghĩa hoàn thành

Một tính năng xong khi: có test cho chuyển trạng thái và ràng buộc liên quan, build backend và frontend đều qua, đã thử trên dữ liệu mẫu, và báo cáo được cập nhật nếu nghiệp vụ thay đổi.
