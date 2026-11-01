// Nhãn tiếng Việt và sắc thái hiển thị cho các giá trị enum từ API.
import type {
  ApplicationStatus,
  CareActivity,
  CareMode,
  CaseStatus,
  ContactMethod,
  FollowUpResult,
  FollowUpStatus,
  HandoverStatus,
  HousingType,
  IntakeSource,
  PetArea,
  PetStatus,
  Priority,
  ReportStatus,
  EndEventType,
  Role,
  Sex,
  Species,
  VetRequestStatus,
} from "@/types/api";

/** Sắc thái của badge: luôn đi kèm chữ, không dùng màu làm tín hiệu duy nhất. */
export type Tone = "neutral" | "info" | "success" | "warning" | "danger" | "primary" | "accent";

export interface Labeled {
  label: string;
  tone: Tone;
}

export const SPECIES: Record<Species, string> = { DOG: "Chó", CAT: "Mèo" };
export const SEX: Record<Sex, string> = { MALE: "Đực", FEMALE: "Cái", UNKNOWN: "Chưa rõ" };
export const ROLE: Record<Role, string> = { ADOPTER: "Người nhận nuôi", STAFF: "Nhân viên", MANAGER: "Quản lý" };
export const AREA: Record<PetArea, string> = { QUARANTINE: "Khu cách ly", GENERAL: "Khu chung" };
export const HOUSING: Record<HousingType, string> = { HOUSE: "Nhà riêng", APARTMENT: "Chung cư", OTHER: "Khác" };
export const CARE_MODE: Record<CareMode, string> = { OUTPATIENT: "Khám ngoại trú", INPATIENT: "Nội trú tại cơ sở" };
export const INTAKE_SOURCE: Record<IntakeSource, string> = {
  RESCUE: "Cứu hộ",
  RETURN: "Trả lại / thu hồi",
  FOUND: "Tìm lại sau thất lạc",
};
export const CARE_ACTIVITY: Record<CareActivity, string> = {
  FEEDING: "Cho ăn",
  CLEANING: "Vệ sinh",
  WEIGHT: "Cân nặng",
  MEDICATION: "Cho thuốc",
  HEALTH_NOTE: "Ghi chú sức khỏe",
  AREA_CHANGE: "Đổi khu",
};

export const END_EVENT: Record<EndEventType, Labeled> = {
  DECEASED: { label: "Qua đời", tone: "neutral" },
  LOST: { label: "Thất lạc", tone: "warning" },
  TRANSFERRED: { label: "Chuyển cơ sở", tone: "info" },
};

export const CONTACT_METHOD: Record<ContactMethod, string> = {
  PHONE: "Gọi điện",
  HOME_VISIT: "Thăm nhà",
  VIDEO_CALL: "Gọi video",
};

export const PRIORITY: Record<Priority, Labeled> = {
  URGENT: { label: "Khẩn cấp", tone: "danger" },
  NEED_CHECK: { label: "Cần kiểm tra", tone: "warning" },
  STABLE: { label: "Ổn định", tone: "success" },
};

export const PET_STATUS: Record<PetStatus, Labeled> = {
  IN_CARE: { label: "Đang chăm sóc", tone: "info" },
  IN_TREATMENT: { label: "Đang điều trị", tone: "warning" },
  AVAILABLE: { label: "Sẵn sàng nhận nuôi", tone: "success" },
  PENDING_HANDOVER: { label: "Chờ bàn giao", tone: "accent" },
  ADOPTED: { label: "Đã có chủ", tone: "primary" },
  DECEASED: { label: "Đã mất", tone: "neutral" },
  LOST: { label: "Thất lạc", tone: "neutral" },
  TRANSFERRED: { label: "Đã chuyển đi", tone: "neutral" },
};

export const REPORT_STATUS: Record<ReportStatus, Labeled> = {
  NEW: { label: "Mới", tone: "accent" },
  PROCESSED: { label: "Đã xử lý", tone: "success" },
  DUPLICATE: { label: "Trùng", tone: "neutral" },
  INVALID: { label: "Không hợp lệ", tone: "neutral" },
};

export const CASE_STATUS: Record<CaseStatus, Labeled> = {
  NEW: { label: "Chưa phân công", tone: "accent" },
  IN_PROGRESS: { label: "Đang xử lý", tone: "info" },
  SUCCEEDED: { label: "Thành công", tone: "success" },
  FAILED: { label: "Thất bại", tone: "danger" },
};

export const VET_STATUS: Record<VetRequestStatus, Labeled> = {
  PENDING: { label: "Chờ khám", tone: "accent" },
  IN_TREATMENT: { label: "Đang điều trị", tone: "warning" },
  COMPLETED: { label: "Hoàn tất", tone: "success" },
  CANCELLED: { label: "Đã hủy", tone: "neutral" },
};

export const APPLICATION_STATUS: Record<ApplicationStatus, Labeled> = {
  PENDING: { label: "Chờ duyệt", tone: "accent" },
  APPROVED: { label: "Đã duyệt", tone: "success" },
  REJECTED: { label: "Từ chối", tone: "danger" },
  CANCELLED: { label: "Đã hủy", tone: "neutral" },
  HANDED_OVER: { label: "Đã bàn giao", tone: "primary" },
};

export const HANDOVER_STATUS: Record<HandoverStatus, Labeled> = {
  IN_PROGRESS: { label: "Đang hậu kiểm", tone: "info" },
  COMPLETED: { label: "Hoàn tất", tone: "success" },
  RECALL_PENDING: { label: "Chờ thu hồi", tone: "danger" },
  RETURNED: { label: "Đã trả lại", tone: "neutral" },
  CLOSED: { label: "Đã đóng", tone: "neutral" },
};

export const FOLLOW_UP_STATUS: Record<FollowUpStatus, Labeled> = {
  SCHEDULED: { label: "Đã lên lịch", tone: "info" },
  DONE: { label: "Đã thực hiện", tone: "success" },
  CANCELLED: { label: "Đã hủy", tone: "neutral" },
};

export const FOLLOW_UP_RESULT: Record<FollowUpResult, Labeled> = {
  PASS: { label: "Đạt", tone: "success" },
  WATCH: { label: "Cần theo dõi", tone: "warning" },
  FAIL: { label: "Không đạt", tone: "danger" },
};

/** Sự kiện đổi trạng thái thú (TransitionTable). Không có trong bảng thì hiện mã gốc. */
export const PET_EVENT: Record<string, string> = {
  RESCUE_SUCCEEDED: "Tiếp nhận từ ca cứu hộ",
  VET_REQUEST_OPENED: "Mở yêu cầu thú y",
  VET_REQUEST_CLOSED: "Kết thúc điều trị",
  ELIGIBILITY_PASSED: "Đạt checklist, sẵn sàng nhận nuôi",
  MARKED_INELIGIBLE: "Đánh dấu không đủ điều kiện",
  APPLICATION_APPROVED: "Duyệt đơn nhận nuôi",
  HANDOVER_CANCELLED: "Hủy bàn giao",
  HANDED_OVER: "Bàn giao cho chủ nuôi",
  RETURNED: "Nhận lại thú",
  FOUND: "Tìm thấy",
  DECEASED: "Đã mất",
  LOST: "Thất lạc",
  TRANSFERRED: "Chuyển đi",
};

export function labelOf<K extends string>(map: Record<K, string>, key: K | null | undefined): string {
  return key ? (map[key] ?? key) : "—";
}
