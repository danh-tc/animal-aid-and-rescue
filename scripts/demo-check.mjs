#!/usr/bin/env node

//

//

const args = process.argv.slice(2);
const API = args.includes("--api") ? args[args.indexOf("--api") + 1] : "http://localhost:8080";
const PASSWORD = "Password@123";

let passed = 0;
let failed = 0;
const tokens = {};

async function call(method, path, { as, body } = {}) {
  const res = await fetch(API + path, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(as ? { Authorization: `Bearer ${tokens[as]}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }
  return { status: res.status, data };
}

function check(label, ok, detail = "") {
  if (ok) {
    passed++;
    console.log(`  ✔ ${label}`);
  } else {
    failed++;
    console.log(`  ✘ ${label}${detail ? `\n      ${detail}` : ""}`);
  }
}

async function expect(label, method, path, opts, status) {
  const res = await call(method, path, opts);
  check(`${label} (${res.status})`, res.status === status,
    res.status === status ? "" : `mong đợi ${status}, nhận ${res.status}: ${JSON.stringify(res.data).slice(0, 300)}`);
  return res.data;
}

async function login(key, email) {
  const res = await call("POST", "/api/public/auth/login", { body: { email, password: PASSWORD } });
  if (res.status !== 200) throw new Error(`Không đăng nhập được ${email}: ${res.status}. Backend đã chạy với profile dev chưa?`);
  tokens[key] = res.data.accessToken;
}

async function main() {
  console.log(`API: ${API}\n`);
  await login("manager", "manager@aar.local");
  await login("staff", "staff1@aar.local");
  await login("staff2", "staff2@aar.local");

  const pet = async (id) => (await call("GET", `/api/staff/pets/${id}`, { as: "staff" })).data;
  const ki = await pet(10);
  if (!ki || ki.status !== "LOST") {
    throw new Error("Dữ liệu mẫu chưa đúng (Ki phải đang Thất lạc). Chạy docker compose down -v && docker compose up -d rồi bật lại backend.");
  }

  console.log("Kịch bản 1. Cứu hộ (UC-07)");
  const look = (await call("GET", "/api/public/rescue-reports/lookup?code=BC-R8N4KD&phone=0911000007")).data;
  const vangStage = look?.cases?.[0]?.pet;
  check("Người báo thấy con vật mình báo đang tìm nhà, có link hồ sơ (UC-02)",
    vangStage?.stage === "LOOKING_FOR_HOME" && vangStage?.petId === 3, JSON.stringify(look));
  const dash = (await call("GET", "/api/staff/dashboard", { as: "staff" })).data;
  check("Bảng công việc cảnh báo ca chưa phân công quá hạn (QĐ-17)", dash?.overdueUnassignedCases >= 1,
    JSON.stringify(dash));
  const exc = (await call("GET", "/api/admin/reports/exceptions", { as: "manager" })).data;
  check("Báo cáo ngoại lệ có ca #11", exc?.unassignedCases?.some((c) => c.caseId === 11));
  await expect("Nhân viên không phân công được ca #11", "POST", "/api/staff/rescue-cases/11/assign",
    { as: "staff", body: { staffId: 2 } }, 403);
  await expect("Quản lý phân công ca #11", "POST", "/api/staff/rescue-cases/11/assign",
    { as: "manager", body: { staffId: 2 } }, 200);
  const created = await expect("Nhân viên tạo ca từ BC-H4T8WX, chưa phân công", "POST", "/api/staff/rescue-reports/2/cases",
    { as: "staff", body: { numberOfAnimals: 1 } }, 201);
  check("Ca mới ở trạng thái Chưa phân công", created?.[0]?.status === "NEW");
  const two = await expect("Quản lý tách BC-7K2M9Q thành 2 ca và phân công (QĐ-01)", "POST", "/api/staff/rescue-reports/1/cases",
    { as: "manager", body: { numberOfAnimals: 2, assignedStaffId: 2 } }, 201);
  check("Tạo đủ 2 ca, đã phân công", two?.length === 2 && two.every((c) => c.status === "IN_PROGRESS"));
  await expect("Nhân viên không phụ trách không đóng được ca #14 (QĐ-17)", "POST", "/api/staff/rescue-cases/14/fail",
    { as: "staff2", body: { reason: "Thử đóng ca của người khác" } }, 403);
  await expect("Gửi cả thú mới và thú thất lạc bị từ chối", "POST", "/api/staff/rescue-cases/14/succeed",
    { as: "staff", body: { pet: { species: "DOG" }, lostPetId: 10, intake: { priority: "STABLE" } } }, 400);
  const found = await expect("Ca #14 thành công với thú thất lạc Ki (S-1b)", "POST", "/api/staff/rescue-cases/14/succeed",
    { as: "staff", body: { lostPetId: 10, intake: { priority: "NEED_CHECK", reason: "Người dân thấy ở Bến Bạch Đằng" } } }, 200);
  check("Không tạo hồ sơ mới, dùng hồ sơ cũ của Ki", found?.petId === 10);
  const kiAfter = await pet(10);
  check("Ki về Đang chăm sóc, có lượt tiếp nhận Tìm lại", kiAfter?.status === "IN_CARE"
    && kiAfter?.intakes?.some((i) => i.source === "FOUND"));

  console.log("\nKịch bản 2. Chăm sóc, thú y (UC-09)");
  const vet = await expect("Tạo yêu cầu thú y cho Mực", "POST", "/api/staff/pets/1/vet-requests",
    { as: "staff", body: { clinicId: 1, reason: "Khám tổng quát", priority: "STABLE", careMode: "OUTPATIENT" } }, 201);
  check("Mực tự sang Đang điều trị (QĐ-06)", vet?.petStatus === "IN_TREATMENT");
  const cancelled = await expect("Hủy yêu cầu, ghi chi phí đã phát sinh 150.000", "POST",
    `/api/staff/vet-requests/${vet?.request?.id}/cancel`,
    { as: "staff", body: { reason: "Phòng khám dừng điều trị", incurredCost: 150000 } }, 200);
  check("Mực về Đang chăm sóc", cancelled?.petStatus === "IN_CARE");
  const costs = (await call("GET", "/api/staff/pets/1/costs", { as: "staff" })).data;
  check("Chi phí của yêu cầu hủy tính vào tổng chi phí (QĐ-14)", Number(costs?.totalTreatmentCost) === 150000,
    JSON.stringify(costs));
  const elig = await expect("Đánh giá checklist của Mực", "PUT", "/api/staff/pets/1/eligibility",
    { as: "staff", body: { vaccinated: true, dewormed: true, sterilized: true, temperamentOk: true } }, 200);
  check("Chưa khám trong lượt tiếp nhận nên chưa Sẵn sàng (QĐ-05, QĐ-07)", elig?.status === "IN_CARE",
    JSON.stringify(elig?.missing));
  const bong = await expect("Ghi kết quả khám cho Bông", "POST", "/api/staff/vet-requests/1/complete",
    { as: "staff", body: { diagnosisSummary: "Đã khỏi", totalCost: 500000, healthOk: true } }, 200);
  check("Bông về Đang chăm sóc", bong?.petStatus === "IN_CARE");

  console.log("\nKịch bản 3. Xét duyệt đơn (UC-10)");
  const app1 = (await call("GET", "/api/staff/applications/1", { as: "staff" })).data;
  const app2 = (await call("GET", "/api/staff/applications/2", { as: "staff" })).data;
  check("Đơn của Thu Hà có cảnh báo từng trả thú", app1?.applicantHistory?.some((f) => f.kind === "RETURNED"));
  check("Đơn của Quốc Bảo có cảnh báo hậu kiểm Không đạt", app2?.applicantHistory?.some((f) => f.kind === "RECALLED"));
  const date = new Date(Date.now() + 2 * 86400000).toISOString().slice(0, 10);
  const [r1, r2] = await Promise.all([
    call("POST", "/api/staff/applications/1/approve", { as: "staff", body: { handoverDate: date } }),
    call("POST", "/api/staff/applications/2/approve", { as: "manager", body: { handoverDate: date } }),
  ]);
  const statuses = [r1.status, r2.status].sort();
  check(`Duyệt đồng thời 2 đơn của Vàng: một thành công, một bị chặn (${statuses.join(", ")})`,
    statuses[0] === 200 && statuses[1] === 409);
  const winner = r1.status === 200 ? 1 : 2;
  const loser = winner === 1 ? 2 : 1;
  const loserApp = (await call("GET", `/api/staff/applications/${loser}`, { as: "staff" })).data;
  check("Đơn còn lại tự đóng (QĐ-09)", loserApp?.application?.status === "CANCELLED");
  await expect("Nhân viên hủy đơn đã duyệt khi người nhận không đến (QĐ-11)", "POST",
    `/api/staff/applications/${winner}/cancel`, { as: "staff", body: { reason: "Không đến theo hẹn" } }, 200);
  check("Vàng trở lại Sẵn sàng", (await pet(3))?.status === "AVAILABLE");

  console.log("\nKịch bản 4. Bàn giao, hậu kiểm, trả lại (UC-11)");
  const handover = await expect("Bàn giao Đốm, ghi khoản ủng hộ", "POST", "/api/staff/applications/3/handover",
    { as: "staff", body: { donationAmount: 100000 } }, 201);
  check("Tạo 2 lịch hậu kiểm", handover?.followUps?.length === 2);
  const scheduled = (await call("GET", "/api/staff/follow-ups", { as: "staff" })).data ?? [];
  const miu = scheduled.find((s) => s.petCode === "TH-2026-0006");
  check("Có lịch hậu kiểm của Miu", !!miu);
  const criteria = { contactMethod: "HOME_VISIT", healthOk: true, housingOk: true, commitmentOk: true, petPresent: true, ownerReachable: true };
  if (miu) {
    const id = miu.followUp.id;
    const watch = await expect("Ghi Cần theo dõi", "POST", `/api/staff/follow-ups/${id}/result`,
      { as: "staff", body: { ...criteria, housingOk: false, result: "WATCH", reason: "Chỗ ở chật" } }, 200);
    const extra = watch?.additionalFollowUp?.id;
    check("Hệ thống thêm lần hậu kiểm bổ sung", !!extra);
    if (extra) {
    await expect("Lần bổ sung không được chọn Cần theo dõi (QĐ-12)", "POST", `/api/staff/follow-ups/${extra}/result`,
      { as: "staff", body: { ...criteria, result: "WATCH", reason: "Vẫn chưa ổn" } }, 400);
    await expect("Lần bổ sung ghi Đạt", "POST", `/api/staff/follow-ups/${extra}/result`,
      { as: "staff", body: { ...criteria, result: "PASS" } }, 200);
    }
  }
  const ret = await expect("Chủ trả lại Miu (QĐ-13)", "POST", "/api/staff/pets/6/returns",
    { as: "staff", body: { reason: "Chủ chuyển nhà", priority: "STABLE" } }, 201);
  check("Miu về Đang chăm sóc", ret?.petStatus === "IN_CARE");
  await expect("Nhân viên không đóng được thu hồi của Na", "POST", "/api/staff/handovers/3/close-recall",
    { as: "staff", body: { reason: "Không liên lạc được" } }, 403);
  const closed = await expect("Quản lý đóng thu hồi của Na (QĐ-18)", "POST", "/api/staff/handovers/3/close-recall",
    { as: "manager", body: { reason: "Không liên lạc được chủ sau 30 ngày" } }, 200);
  check("Bàn giao Đã đóng, Na giữ Đã nhận nuôi", closed?.status === "CLOSED" && (await pet(9))?.status === "ADOPTED");

  console.log(`\nKết quả: ${passed} đạt, ${failed} lỗi`);
  process.exit(failed ? 1 : 0);
}

main().catch((e) => {
  console.error(`\nDừng: ${e.message}`);
  process.exit(1);
});
