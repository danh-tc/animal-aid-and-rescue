# Kịch bản demo

Kịch bản đi qua toàn bộ vòng đời trong báo cáo (mục 2.1), dùng dữ liệu mẫu của profile `dev`. Mất khoảng 20 phút. Các bước được sắp để bước sau không phá dữ liệu bước trước cần.

## Chuẩn bị

1. Database sạch để dữ liệu mẫu được nạp lại:

```bash
docker compose down -v && docker compose up -d
```

2. Backend với profile dev. Muốn demo đăng nhập Google thì đặt thêm `GOOGLE_CLIENT_ID` (xem README):

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

3. Frontend:

```bash
cd frontend && npm run dev
```

4. Mở hai cửa sổ trình duyệt (một cửa sổ thường, một cửa sổ ẩn danh) để đăng nhập hai tài khoản cùng lúc ở bước 5.

## Tài khoản

Mật khẩu chung: `Password@123`. Đăng nhập tại `http://localhost:3000/login`.

| Email | Họ tên | Vai trò | Dùng ở bước |
| --- | --- | --- | --- |
| *(không cần)* | Khách | Khách | 1 |
| `manager@aar.local` | Nguyễn Văn Quân | Quản lý | 2, 5, 11. Vận hành: luôn cần ít nhất một Quản lý để phân công ca |
| `staff1@aar.local` | Trần Thị Lan | Nhân viên | 2, 4, 5, 6, 7, 9, 10 |
| `staff2@aar.local` | Lê Minh Tuấn | Nhân viên | 3, 5 |
| `adopter1@aar.local` | Phạm Thu Hà | Người nhận nuôi | 5, 8 |
| `adopter2@aar.local` | Võ Quốc Bảo | Người nhận nuôi | 5, 8 |
| `adopter3@aar.local` | Đặng Ngọc Mai | Người nhận nuôi | 6 |

## Dữ liệu mẫu đáng chú ý

| Thú | Trạng thái lúc đầu | Dùng để demo |
| --- | --- | --- |
| Mực (chó đen) | Đang chăm sóc, khu cách ly | Thất lạc và tìm lại (bước 9) |
| Bông (mèo trắng xám) | Đang điều trị, lưu trú phòng khám | Ghi kết quả thú y (bước 4) |
| Vàng (shiba) | Sẵn sàng, 2 đơn đang chờ của Thu Hà và Quốc Bảo | Duyệt đơn đồng thời (bước 5) |
| Mướp (mèo vằn) | Sẵn sàng | Nộp đơn, không đủ điều kiện (bước 8, 10) |
| Đốm (dalmatian) | Chờ bàn giao, đơn của Ngọc Mai đã duyệt, hẹn bàn giao sau 2 ngày | Bàn giao, 3 con số chi phí (bước 6) |
| Miu (mèo trắng) | Đã nhận nuôi (Quốc Bảo), lần hậu kiểm 2 đã lên lịch | Hậu kiểm (bước 7) |
| Lu (chó nâu) | Đang chăm sóc sau khi Thu Hà trả lại | Lịch sử bàn giao, lượt tiếp nhận RETURN, cảnh báo ở đơn của Thu Hà (bước 5) |
| Tí (mèo tam thể) | Đã mất | Trạng thái kết thúc, hồ sơ vẫn giữ |

Báo cáo mới: `BC-7K2M9Q` (chợ Bến Thành) và `BC-H4T8WX` (công viên Gia Định). Ca đang xử lý: ca #4 ở cầu Ánh Sao, giao cho Lê Minh Tuấn.

## Các bước

### 1. Khách báo cứu hộ và tra cứu (P03, P04, FR-01, FR-02)

Không đăng nhập.

1. Trang chủ: hai bé Sẵn sàng có ảnh thật (Vàng, Mướp), lọc Chó/Mèo.
2. Bấm **Báo cứu hộ**, để trống rồi bấm Gửi để thấy lỗi ngay dưới từng ô.
3. Điền tên, số điện thoại, vị trí "Cầu Ánh Sao, Quận 7", mô tả, chọn một ảnh, gửi. Ghi lại **mã BC-** hiện ra.
4. Bấm **tra cứu tiến độ**, nhập mã và số điện thoại vừa dùng: trạng thái Mới. Nhập sai số điện thoại thì báo không tìm thấy (RB-15).

### 2. Xử lý báo cáo, chỉ Quản lý phân công (S02, S03, QĐ-01, UC-07)

1. Đăng nhập `staff1@aar.local`. Bảng công việc hiện số báo cáo mới.
2. Mở `BC-H4T8WX`, bấm **Tạo ca cứu hộ**: Nhân viên không có ô phân công, ca tạo ra ở trạng thái Chưa phân công. Mở ca vừa tạo: thông báo "Chờ Quản lý phân công", không có nút Phân công.
3. Mở báo cáo khách gửi ở bước 1: khung **Ca đang mở ở gần** có ca #4 ở cầu Ánh Sao. Bấm **Gộp vào ca này**: báo cáo chuyển sang Trùng (ngoại lệ 2a của UC-07).
4. Đăng xuất, đăng nhập `manager@aar.local`. Mở ca vừa tạo ở bước 2, bấm **Phân công** cho Trần Thị Lan.
5. Mở `BC-7K2M9Q`, tạo **2 ca** và chọn phân công luôn: minh họa báo cáo nhiều thú tách thành nhiều ca (QĐ-01).

### 3. Cứu thành công, tạo hồ sơ có ảnh (S03, UC-07 S-1, S-1a)

1. Đăng nhập `staff2@aar.local`. Mở **Ca cứu hộ** → ca #4 (cầu Ánh Sao).
2. Bấm **Cứu thành công**: loài Chó, tên tùy ý, **chọn ảnh**, mức ưu tiên **Khẩn cấp**, đánh giá ban đầu.
3. Hệ thống tạo hồ sơ TH-… ở khu cách ly và hiện nhắc "Thú ở mức Khẩn cấp" kèm nút tạo yêu cầu thú y (S-1a).
4. Muốn demo cứu thất bại: đăng nhập `staff1@aar.local`, mở ca đã được phân công ở bước 2, bấm **Thất bại**: bắt buộc ghi lý do, không tạo hồ sơ (QĐ-02).

### 4. Chăm sóc và thú y (S05, S06, QĐ-05, QĐ-06, QĐ-07)

1. Đăng nhập `staff1@aar.local`. Mở hồ sơ **Bông**: trạng thái Đang điều trị, vị trí là phòng khám (lưu trú).
2. Tab **Thú y** → yêu cầu đang mở → **Ghi kết quả**: chẩn đoán, **Kết luận sức khỏe** ổn hoặc chưa ổn (bắt buộc), chi phí (bắt buộc), chọn ảnh hoặc PDF phiếu khám. Bông tự về Đang chăm sóc, vị trí về Trung tâm (QĐ-06).
3. Tab **Nhật ký**: ghi cân nặng. Tab **Checklist**: các mục còn thiếu kèm lý do (chưa hết cách ly, chưa đủ tuổi…). Mục Sức khỏe dựa trên kết luận của lần khám gần nhất: chọn "chưa ổn" thì mục này không đạt. Đủ 6 mục mới chuyển sang Sẵn sàng (QĐ-07).
4. Tab **Lịch sử**: mọi lần đổi trạng thái kèm người thực hiện (NFR-03).

### 5. Duyệt đơn đồng thời (S07, QĐ-09, RB-01)

1. Cửa sổ thường đăng nhập `staff1@aar.local`, cửa sổ ẩn danh đăng nhập `manager@aar.local`.
2. Cả hai mở **Đơn nhận nuôi** → hai đơn của **Vàng** (mỗi cửa sổ một đơn), xem khung "Các đơn khác cho bé này". Đơn của Thu Hà có cảnh báo vàng "từng trả lại thú" (Lu) kèm liên kết.
3. Bấm **Duyệt đơn**, chọn ngày hẹn bàn giao (mặc định sau 2 ngày), xác nhận gần như cùng lúc: một bên thành công, bên kia báo "Bé đã có đơn khác được duyệt". Đơn còn lại tự đóng.
4. Bên thắng thấy ngày hẹn và nút **Đổi ngày hẹn**. Đăng nhập người có đơn bị đóng (`adopter1` hoặc `adopter2`), vào **Đơn của tôi**: đơn Đã hủy kèm lý do "Hệ thống tự đóng"; người được duyệt thấy "Hẹn bàn giao ngày …".

### 6. Bàn giao, 3 con số chi phí (S08, QĐ-14, UC-11)

1. `staff1@aar.local` mở đơn của **Đốm** (Đã duyệt, có ngày hẹn bàn giao) → **Bàn giao**.
2. Hộp thoại hiện 3 con số: tổng chi phí điều trị, đã ủng hộ, còn chưa bù đắp. Nhập khoản ủng hộ, xác nhận.
3. Đốm chuyển Đã nhận nuôi, hệ thống tạo 2 lịch hậu kiểm (14 và 60 ngày).
4. Đăng nhập `adopter3@aar.local` → **Thú đã nhận**: thấy Đốm và lịch hậu kiểm.

### 7. Hậu kiểm (S09, QĐ-12)

1. `staff1@aar.local` → **Hậu kiểm** → **Tất cả đã lên lịch**.
2. Chọn lần hậu kiểm của **Miu** → **Ghi kết quả**: hình thức **Thăm nhà**, kết quả **Cần theo dõi** (bắt buộc nhận xét): hệ thống thêm một lần hậu kiểm bổ sung.
3. Muốn demo thu hồi: chọn **Không đạt** → bàn giao chuyển Chờ thu hồi → mở hồ sơ thú, tab **Bàn giao** → **Tiếp nhận trả lại** (S10, QĐ-13): tạo lượt tiếp nhận RETURN, thú về khu cách ly. Kết quả do nhân viên tự chọn theo bảng tiêu chí ở 2.1.5. Từ đây mọi đơn mới của Quốc Bảo hiện cảnh báo đỏ "từng bị thu hồi thú".

### 8. Người nhận nuôi (P05, A01, A02, A03)

1. **Đăng ký** tài khoản mới (hoặc **Đăng ký bằng Google** nếu đã cấu hình). Tài khoản chưa có số điện thoại hoặc địa chỉ thì form nộp đơn hiện thêm hai ô này, bắt buộc nhập (QĐ-08).
2. Hoặc đăng nhập `adopter1@aar.local`, mở **Mướp** → **Nộp đơn nhận nuôi**: chọn nhà ở, kinh nghiệm. Nộp lần hai cho cùng bé thì bị chặn (RB-02). Mỗi người tối đa 5 đơn đang chờ (tham số `MAX_PENDING_APPLICATIONS`).
3. **Đơn của tôi**: theo dõi, hủy đơn. Đăng nhập `adopter2@aar.local` → **Thú đã nhận**: Miu và lịch hậu kiểm.
4. Thử vào `/staff` bằng tài khoản người nhận nuôi: bị chuyển về trang của mình (phân quyền theo vai trò).

### 9. Thất lạc và tìm lại (S10, QĐ-15, QĐ-16)

1. `staff1@aar.local` mở hồ sơ **Mực** → **Sự kiện kết thúc** → Thất lạc, ghi nơi mất. Nếu thú còn yêu cầu thú y đang mở, hộp thoại liệt kê chúng và bắt tích xác nhận đã ghi chi phí cần thiết trước khi lưu.
2. Hồ sơ hiện thông báo thất lạc, các việc đang mở tự đóng.
3. Bấm **Tìm lại được**: tạo lượt tiếp nhận "Tìm lại sau thất lạc", Mực về Đang chăm sóc, khu cách ly.

### 10. Thú không đủ điều kiện (QĐ-10)

1. Sau bước 8, Mướp có đơn chờ của Thu Hà.
2. `staff1@aar.local` mở hồ sơ **Mướp** → **Không đủ điều kiện**, ghi lý do: Mướp về Đang chăm sóc, đơn chờ tự hủy, Mướp biến khỏi trang công khai.

### 11. Quản lý (M01–M04)

1. Đăng nhập `manager@aar.local` → **Báo cáo thống kê**: tab Tổng hợp (biểu đồ theo tháng, rê chuột xem số), Ngoại lệ, Chi phí và ủng hộ (theo tháng, cơ sở thú y, từng thú). Bấm **Xem dạng bảng**.
2. **Tài khoản**: tạo nhân viên mới, khóa tài khoản. Không tự khóa được chính mình.
3. **Cơ sở thú y**: thêm, tắt hợp tác. **Tham số**: đổi `QUARANTINE_DAYS` rồi xem checklist của một thú thay đổi theo.

## Nếu bị hỏi thêm

- Kiểm thử tự động: `cd backend && mvn test` (147 test trên PostgreSQL thật).
- Đo hiệu năng NFR-04: `node scripts/perf-check.mjs`.
- Sao lưu và khôi phục NFR-06: `./scripts/backup.sh`, `./scripts/restore.sh backups/<tệp>.dump`.
- Tài liệu API: `http://localhost:8080/swagger-ui.html`.
