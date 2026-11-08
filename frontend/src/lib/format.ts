const dateFmt = new Intl.DateTimeFormat("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
const dateTimeFmt = new Intl.DateTimeFormat("vi-VN", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});
const moneyFmt = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 });

export function formatDate(value: string | null | undefined): string {
  if (!value) return "—";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : dateFmt.format(d);
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return "—";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : dateTimeFmt.format(d);
}

export function formatMoney(value: number | null | undefined): string {
  return value === null || value === undefined ? "—" : moneyFmt.format(value);
}

export function ageLabel(birthDate: string | null | undefined): string | null {
  if (!birthDate) return null;
  const months = Math.floor((Date.now() - Date.parse(birthDate)) / (1000 * 60 * 60 * 24 * 30.4));
  if (Number.isNaN(months)) return null;
  if (months < 12) return `Khoảng ${Math.max(months, 1)} tháng tuổi`;
  return `Khoảng ${Math.floor(months / 12)} tuổi`;
}

export function daysUntil(date: string): number {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const due = new Date(`${date}T00:00:00`);
  return Math.round((due.getTime() - today.getTime()) / 86_400_000);
}

export function todayIso(offsetDays = 0): string {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${m}-${day}`;
}

export function petName(name: string | null | undefined, code?: string): string {
  return name?.trim() ? name : code ? `Bé ${code}` : "Chưa đặt tên";
}

const compactFmt = new Intl.NumberFormat("vi-VN", { notation: "compact", maximumFractionDigits: 1 });

export function formatCompact(value: number): string {
  return compactFmt.format(value);
}

/** "2026-03" => "T3/2026" */
export function formatMonth(month: string): string {
  const [y, m] = month.split("-");
  return `T${Number(m)}/${y}`;
}
