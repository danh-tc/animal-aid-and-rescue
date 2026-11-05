# Kịch bản demo

Demo đi theo 4 Use Case chính đã đặc tả ở báo cáo mục 2.5 (kịch bản ở mục 5.3), mỗi Use Case một kịch bản, dùng dữ liệu mẫu của profile `dev`. Bốn kịch bản chính mất khoảng 15 phút, phần bổ sung ở cuối dùng khi còn thời gian hoặc khi được hỏi.

## Chuẩn bị

1. Database sạch để dữ liệu mẫu được nạp lại (dữ liệu mẫu chỉ nạp khi bảng tài khoản còn trống):

```bash
docker compose down -v && docker compose up -d
```

2. Backend với profile dev:

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

3. Frontend:

```bash
cd frontend && npm run dev
```

4. Mở hai cửa sổ trình duyệt (một cửa sổ thường, một cửa sổ ẩn danh) để đăng nhập hai tài khoản cùng lúc ở kịch bản 3.

## Tài khoản

Mật khẩu chung: `Password@123`. Đăng nhập tại `http://localhost:3000/login`.

| Email | Họ tên | Vai trò |
| --- | --- | --- |
| `manager@aar.local` | Nguyễn Văn Quân | Quản lý |
| `staff1@aar.local` | Trần Thị Lan | Nhân viên |
| `staff2@aar.local` | Lê Minh Tuấn | Nhân viên |
| `adopter1@aar.local` | Phạm Thu Hà | Người nhận nuôi (từng trả lại Lu) |
| `adopter2@aar.local` | Võ Quốc Bảo | Người nhận nuôi (đang nuôi Miu, hậu kiểm Na không đạt) |
| `adopter3@aar.local` | Đặng Ngọc Mai | Người nhận nuôi (đơn nhận Đốm đã duyệt) |

## Dữ liệu mẫu đáng chú ý

| Dữ liệu | Trạng thái lúc đầu | Dùng ở |
| --- | --- | --- |
| Báo cáo `BC-7K2M9Q` (chợ Bến Thành), `BC-H4T8WX` (công viên Gia Định) | Mới | Kịch bản 1 |
| Ca #4 (cầu Ánh Sao), giao Lê Minh Tuấn | Đang xử lý | Kịch bản 1, gộp trùng và cứu thành công |
| Ca #11 (ngã tư Hàng Xanh) | Chưa phân công 4 giờ | Kịch bản 1, cảnh báo quá hạn (QĐ-17) |
| Ca #14 (Bến Bạch Đằng), giao Trần Thị Lan | Đang xử lý | Kịch bản 1, tìm lại thú thất lạc (S-1b) |
| Ki (chó xám, vòng cổ đỏ) | Thất lạc | Kịch bản 1, S-1b |
| Bông (mèo trắng xám) | Đang điều trị, lưu trú phòng khám | Kịch bản 2 |
| Mực (chó đen) | Đang chăm sóc, khu cách ly | Kịch bản 2, checklist chưa đạt |
| Vàng (shiba) | Sẵn sàng, 2 đơn chờ của Thu Hà và Quốc Bảo | Kịch bản 3 |
| Đốm (dalmatian) | Chờ bàn giao, đơn của Ngọc Mai đã duyệt | Kịch bản 4, bàn giao |
| Miu (mèo trắng) | Đã nhận nuôi, lần hậu kiểm 2 đã lên lịch | Kịch bản 4, hậu kiểm |
| Na (mèo xám) | Đã nhận nuôi, hậu kiểm Không đạt 35 ngày trước, chờ thu hồi | Kịch bản 4, đóng thu hồi (QĐ-18) |
| Lu (chó nâu) | Đang chăm sóc sau khi Thu Hà trả lại | Cảnh báo ở đơn của Thu Hà |
| Mướp (mèo vằn) | Sẵn sàng | Phần bổ sung |
| Tí (mèo tam thể) | Đã mất | Trạng thái kết thúc, hồ sơ vẫn giữ |

## Kịch bản 1. Cứu hộ (UC-07)

Điểm cần cho thấy: tách và gộp báo cáo, chỉ Quản lý phân công, ca thành công tạo hồ sơ, tìm lại thú thất lạc qua ca cứu hộ.

1. **Khách báo cứu hộ** (không đăng nhập): bấm **Báo cứu hộ**, để trống rồi gửi để thấy lỗi dưới từng ô. Điền vị trí "Cầu Ánh Sao, Quận 7", gửi, ghi lại mã BC-. Bấm **tra cứu tiến độ** bằng mã và số điện thoại: trạng thái Mới. Sai số điện thoại thì báo không tìm thấy (RB-15).
2. Đăng nhập `staff1@aar.local`. Bảng công việc có cảnh báo **ca cứu hộ chưa được phân công quá hạn** (ca #11, QĐ-17) kèm câu "Chỉ Quản lý phân công được".
3. Mở báo cáo vừa gửi: khung **Ca đang mở để kiểm tra trùng** có ca #4 ở cầu Ánh Sao. Bấm **Gộp vào ca này**: báo cáo chuyển Trùng (ngoại lệ 2a).
4. Mở `BC-H4T8WX`, **Tạo ca cứu hộ**: Nhân viên không có ô phân công. Mở ca vừa tạo: thông báo "Chờ Quản lý phân công", không có nút Phân công.
5. Đăng nhập `manager@aar.local`. Mở `BC-7K2M9Q`, tạo **2 ca** và phân công luôn (báo cáo nhiều thú tách thành nhiều ca, QĐ-01). Mở ca #11, **Phân công**: cảnh báo trên bảng công việc biến mất.
6. Đăng nhập `staff2@aar.local`, mở ca #4 → **Cứu thành công** → **Thú mới**: loài Chó, chọn ảnh, mức ưu tiên **Khẩn cấp**. Hệ thống tạo hồ sơ TH-… ở khu cách ly và nhắc tạo yêu cầu thú y (S-1a).
7. Đăng nhập `staff1@aar.local`, mở ca #14 (Bến Bạch Đằng) → **Cứu thành công** → **Thú thất lạc của trung tâm**, chọn **Ki**: không tạo hồ sơ mới, Ki từ Thất lạc về Đang chăm sóc, tab **Lượt tiếp nhận** có lượt "Tìm lại" gắn với ca #14 (S-1b, QĐ-16).
8. Muốn demo cứu thất bại: mở một ca đã phân công, bấm **Thất bại**: bắt buộc ghi lý do, không tạo hồ sơ (QĐ-02).

## Kịch bản 2. Chăm sóc, thú y (UC-09)

Điểm cần cho thấy: thú tự đổi trạng thái theo yêu cầu thú y, chi phí của yêu cầu hủy vẫn được tính, checklist chặn chuyển Sẵn sàng.

1. `staff1@aar.local` mở hồ sơ **Bông**: Đang điều trị, vị trí là bệnh viện thú y (lưu trú).
2. Tab **Thú y** → yêu cầu đang mở → **Ghi kết quả**: chẩn đoán, **Kết luận sức khỏe** (bắt buộc), chi phí (bắt buộc), phiếu khám. Bông tự về Đang chăm sóc, vị trí về Trung tâm (QĐ-06).
3. Mở hồ sơ **Mực**, tab **Thú y** → **Tạo yêu cầu**: Mực tự sang Đang điều trị. Mở yêu cầu vừa tạo → **Hủy**: ghi lý do "Phòng khám dừng điều trị" và **chi phí đã phát sinh** 150.000 đồng. Mực về Đang chăm sóc, chi phí hủy vẫn có trong tổng chi phí điều trị (QĐ-14).
4. Tab **Checklist** của Mực: mục Sức khỏe không đạt vì chưa có lần khám nào kết luận ổn trong lượt tiếp nhận hiện tại (QĐ-05), mục cách ly chưa đạt vì mới vào 3 ngày. Thiếu mục nào thì không chuyển được Sẵn sàng (QĐ-07).
5. Tab **Lịch sử**: mọi lần đổi trạng thái kèm người thực hiện (NFR-03).

## Kịch bản 3. Xét duyệt đơn (UC-10)

Điểm cần cho thấy: cảnh báo lịch sử người nộp, hai nhân viên duyệt cùng lúc chỉ một người thành công.

1. Muốn có bước nộp đơn (UC-05): đăng nhập `adopter1@aar.local`, mở **Mướp** → **Nộp đơn nhận nuôi**. Nộp lần hai cho cùng bé thì bị chặn (RB-02).
2. Cửa sổ thường đăng nhập `staff1@aar.local`, cửa sổ ẩn danh đăng nhập `manager@aar.local`. Cả hai mở **Đơn nhận nuôi** → hai đơn của **Vàng**, mỗi cửa sổ một đơn.
3. Đơn của Thu Hà có cảnh báo "Đã trả lại thú" (Lu). Đơn của Quốc Bảo có cảnh báo "Hậu kiểm Không đạt" (Na). Hệ thống chỉ cảnh báo, không chặn, nhân viên tự đánh giá theo 5 tiêu chí thẩm định.
4. Hai bên bấm **Duyệt đơn**, chọn ngày hẹn bàn giao, xác nhận gần như cùng lúc: một bên thành công, bên kia báo "Bé đã có đơn khác được duyệt". Đơn còn lại tự đóng (QĐ-09, RB-01).
5. Đăng nhập người có đơn bị đóng, vào **Đơn của tôi**: đơn Đã hủy kèm lý do "Hệ thống tự đóng". Người được duyệt thấy "Hẹn bàn giao ngày …".

## Kịch bản 4. Bàn giao, hậu kiểm, trả lại (UC-11)

Điểm cần cho thấy: chi phí khi bàn giao, quy tắc hậu kiểm bổ sung, trả lại thú, Quản lý đóng thu hồi.

1. `staff1@aar.local` mở đơn của **Đốm** (Đã duyệt) → **Bàn giao**: hộp thoại hiện tổng chi phí điều trị, số tiền đã được ủng hộ và phần còn thiếu. Nhập khoản ủng hộ, xác nhận. Đốm chuyển Đã nhận nuôi, hệ thống tạo 2 lịch hậu kiểm (14 và 60 ngày).
2. **Hậu kiểm** → **Tất cả đã lên lịch** → lần hậu kiểm của **Miu** → **Ghi kết quả**, hình thức **Thăm nhà**:
   - Bỏ tích một tiêu chí: lựa chọn **Đạt** biến mất (Đạt phải đủ 5 tiêu chí).
   - Chọn **Cần theo dõi**, ghi nhận xét: hệ thống thêm một lần hậu kiểm bổ sung.
   - Mở lần bổ sung: chỉ còn **Đạt** hoặc **Không đạt** (QĐ-12).
3. Mở hồ sơ Miu, tab **Bàn giao** → **Tiếp nhận trả lại**: tạo lượt tiếp nhận trả lại, Miu về khu cách ly, lịch hậu kiểm còn lại bị hủy, lịch sử nhận nuôi vẫn giữ (QĐ-13).
4. Mở hồ sơ **Na**, tab **Bàn giao**: thông báo "Hậu kiểm không đạt, đang chờ thu hồi". Với `staff1@aar.local` không có nút đóng. Đăng nhập `manager@aar.local`: có nút **Đóng thu hồi**, ghi lý do, bàn giao chuyển Đã đóng, Na giữ Đã nhận nuôi (QĐ-18). Báo cáo **Ngoại lệ** có cột "Được đóng thu hồi từ".
5. Đăng nhập `adopter3@aar.local` → **Thú đã nhận**: thấy Đốm và lịch hậu kiểm.

## Phần bổ sung

- **Thú không đủ điều kiện (QĐ-10):** sau kịch bản 3, Mướp có đơn chờ của Thu Hà. Mở hồ sơ Mướp → **Không đủ điều kiện**: Mướp về Đang chăm sóc, đơn chờ tự hủy.
- **Người nhận không đến bàn giao (QĐ-11):** mở một đơn Đã duyệt → **Hủy do không đến**, ghi lý do: thú về Sẵn sàng.
- **Thất lạc và qua đời (QĐ-15, QĐ-16):** mở hồ sơ thú → **Sự kiện kết thúc**. Nếu thú còn yêu cầu thú y đang mở, hộp thoại có ô nhập chi phí đã phát sinh cho từng yêu cầu. Thú thất lạc thì bấm **Tìm lại được** để tạo lượt tiếp nhận mới.
- **Người nhận nuôi:** đăng ký tài khoản mới. Tài khoản chưa có số điện thoại hoặc địa chỉ thì form nộp đơn bắt buộc nhập (QĐ-08). Vào `/staff` bằng tài khoản người nhận nuôi thì bị chuyển về trang của mình.
- **Quản lý (M01 đến M04):** **Báo cáo thống kê** (Tổng hợp, Ngoại lệ, Chi phí và ủng hộ), **Tài khoản**, **Cơ sở thú y**, **Tham số** (đổi `QUARANTINE_DAYS` rồi xem checklist thay đổi theo).

## Nếu bị hỏi thêm

- Kiểm thử tự động: `cd backend && mvn verify` (test tích hợp chạy trên PostgreSQL thật qua Testcontainers, cần Docker).
- Đo hiệu năng NFR-04: `node scripts/perf-check.mjs`.
- Sao lưu và khôi phục NFR-06: `./scripts/backup.sh`, `./scripts/restore.sh backups/<tệp>.dump`.
- Tài liệu API: `http://localhost:8080/swagger-ui.html`.
