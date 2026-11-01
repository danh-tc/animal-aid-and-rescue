#!/usr/bin/env node
// Đo NFR-04 ("thao tác thường phản hồi trong khoảng 2 giây với dữ liệu demo"): gọi lặp các API
// chính và in thời gian phản hồi p50, p95, lớn nhất (ms). Cần backend đang chạy với dữ liệu demo.
//
//   node scripts/perf-check.mjs                    # mặc định 30 lần mỗi thao tác
//   node scripts/perf-check.mjs --runs 50 --api http://localhost:8080
const args = process.argv.slice(2);
const opt = (name, fallback) => {
  const i = args.indexOf(`--${name}`);
  return i >= 0 ? args[i + 1] : fallback;
};
const API = opt("api", "http://localhost:8080");
const RUNS = Number(opt("runs", "30"));
const LIMIT_MS = 2000;

async function call(path, { token, method = "GET", body } = {}) {
  const res = await fetch(API + path, {
    method,
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  await res.arrayBuffer();
  if (!res.ok) throw new Error(`${method} ${path} => ${res.status}`);
  return res;
}

async function login(email) {
  const res = await fetch(`${API}/api/public/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password: "Password@123" }),
  });
  return (await res.json()).accessToken;
}

function pct(sorted, p) {
  return sorted[Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1)];
}

async function measure(label, fn) {
  await fn(); // làm nóng: lần đầu JVM còn biên dịch
  const times = [];
  for (let i = 0; i < RUNS; i++) {
    const t = performance.now();
    await fn(i);
    times.push(performance.now() - t);
  }
  times.sort((a, b) => a - b);
  return { label, p50: pct(times, 50), p95: pct(times, 95), max: times[times.length - 1] };
}

const staff = await login("staff1@aar.local");
const manager = await login("manager@aar.local");
const pets = (await (await fetch(`${API}/api/public/pets`)).json()).items;
const petId = pets[0]?.id ?? 3;

const rows = [];
rows.push(await measure("P01 danh sách thú công khai", () => call("/api/public/pets")));
rows.push(await measure("P02 chi tiết thú", () => call(`/api/public/pets/${petId}`)));
rows.push(await measure("Đăng nhập", () => login("staff1@aar.local")));
rows.push(await measure("S01 bảng công việc", () => call("/api/staff/dashboard", { token: staff })));
rows.push(await measure("S04 danh sách thú (lọc, tìm)", () => call("/api/staff/pets?status=IN_CARE&q=a", { token: staff })));
rows.push(await measure("S05 hồ sơ thú đầy đủ", () => call(`/api/staff/pets/${petId}`, { token: staff })));
rows.push(await measure("S07 danh sách đơn", () => call("/api/staff/applications?status=PENDING", { token: staff })));
rows.push(await measure("S05 ghi nhật ký chăm sóc", () =>
  call(`/api/staff/pets/1/care-logs`, { token: staff, method: "POST", body: { activityType: "FEEDING", note: "Đo hiệu năng" } })));
rows.push(await measure("M01 báo cáo tổng hợp 12 tháng", () => call("/api/admin/reports/summary", { token: manager })));
rows.push(await measure("M01 báo cáo chi phí", () => call("/api/admin/reports/costs", { token: manager })));

const fmt = (n) => n.toFixed(0).padStart(6);
console.log(`NFR-04: ${RUNS} lần mỗi thao tác, ngưỡng ${LIMIT_MS} ms\n`);
console.log("Thao tác".padEnd(34) + "   p50    p95    max  (ms)");
for (const r of rows) console.log(r.label.padEnd(34) + fmt(r.p50) + " " + fmt(r.p95) + " " + fmt(r.max));
const worst = Math.max(...rows.map((r) => r.max));
console.log(`\n${worst <= LIMIT_MS ? "ĐẠT" : "KHÔNG ĐẠT"}: thao tác chậm nhất ${worst.toFixed(0)} ms`);
process.exit(worst <= LIMIT_MS ? 0 : 1);
