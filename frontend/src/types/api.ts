// Kiểu dữ liệu khớp API Spring. Sau này thay bằng client sinh từ springdoc-openapi.

export type Species = "DOG" | "CAT";
export type Sex = "MALE" | "FEMALE" | "UNKNOWN";
export type Role = "ADOPTER" | "STAFF" | "MANAGER";

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface PublicPet {
  id: number;
  code: string;
  name: string | null;
  species: Species;
  sex: Sex;
  estimatedBirthDate: string | null;
  color: string | null;
  description: string | null;
  /** false: đơn nhận nuôi phải kèm cam kết triệt sản */
  sterilized: boolean;
  /** Ảnh: GET /api/public/pets/{id}/photos/{photoId} */
  photoIds: number[];
}

export interface UserView {
  id: number;
  email: string;
  fullName: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  expiresInMinutes: number;
  user: UserView;
}

export interface ApiErrorBody {
  code: string;
  message: string;
  fieldErrors?: Record<string, string>;
}
