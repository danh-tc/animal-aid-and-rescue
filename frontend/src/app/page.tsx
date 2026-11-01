import { PetCard } from "@/components/pet-card/PetCard";
import { ApiRequestError, apiFetch } from "@/lib/api";
import type { PageResponse, PublicPet } from "@/types/api";
import "./home.scss";

// Danh sách thay đổi liên tục, luôn lấy dữ liệu mới khi có request (màn hình P01)
export const dynamic = "force-dynamic";

async function loadPets(): Promise<{ data: PageResponse<PublicPet> | null; error: string | null }> {
  try {
    const data = await apiFetch<PageResponse<PublicPet>>("/api/public/pets", { cache: "no-store" });
    return { data, error: null };
  } catch (e) {
    const message = e instanceof ApiRequestError ? e.message : "Không kết nối được tới máy chủ";
    return { data: null, error: message };
  }
}

export default async function HomePage() {
  const { data, error } = await loadPets();

  return (
    <div className="container">
      <section className="hero">
        <h1 className="hero__title">Mỗi bé được cứu đều xứng đáng có một mái nhà</h1>
        <p className="hero__lead">
          Các bé dưới đây đã được khám thú y, tiêm phòng và sẵn sàng về nhà mới. Đăng nhập để nộp đơn nhận nuôi.
        </p>
      </section>

      <section className="pet-list" aria-labelledby="pet-list-title">
        <div className="pet-list__header">
          <h2 id="pet-list-title">Đang chờ nhận nuôi</h2>
          {data && <span className="pet-list__count">{data.totalItems} bé</span>}
        </div>

        {error && <p className="notice notice--error">{error}</p>}
        {data && data.items.length === 0 && <p className="notice">Hiện chưa có bé nào sẵn sàng nhận nuôi.</p>}
        {data && data.items.length > 0 && (
          <div className="pet-list__grid">
            {data.items.map((pet) => (
              <PetCard key={pet.id} pet={pet} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
