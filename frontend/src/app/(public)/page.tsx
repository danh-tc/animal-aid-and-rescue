import { ArrowRight, ClipboardCheck, HeartHandshake, HousePlus, Search, ShieldCheck, Siren, Stethoscope } from "lucide-react";
import Link from "next/link";
import { LinkButton } from "@/components/button/Button";
import { EmptyState } from "@/components/empty-state/EmptyState";
import { Notice } from "@/components/notice/Notice";
import { PetCard } from "@/components/pet-card/PetCard";
import { apiFetch, errorMessage } from "@/lib/api";
import type { PageResponse, PublicPet, Species } from "@/types/api";
import "./home.scss";

export const dynamic = "force-dynamic";

const PAGE_SIZE = 12;
const FILTERS: { value: Species | ""; label: string }[] = [
  { value: "", label: "Tất cả" },
  { value: "DOG", label: "Chó" },
  { value: "CAT", label: "Mèo" },
];

const STEPS = [
  { icon: Search, title: "Chọn bé phù hợp", text: "Xem hồ sơ, ảnh và tính cách của các bé đã sẵn sàng." },
  { icon: ClipboardCheck, title: "Nộp đơn nhận nuôi", text: "Điền thông tin chỗ ở và kinh nghiệm nuôi thú cưng." },
  { icon: HeartHandshake, title: "Duyệt và bàn giao", text: "Nhân viên liên hệ, xét duyệt và hẹn ngày đón bé về." },
  { icon: HousePlus, title: "Đồng hành sau nhận nuôi", text: "Trung tâm theo dõi sau bàn giao để chắc chắn bé được chăm sóc tốt." },
];

interface HomeProps {
  searchParams: Promise<{ species?: string; page?: string }>;
}

async function loadPets(species: Species | "", page: number) {
  try {
    const data = await apiFetch<PageResponse<PublicPet>>("/api/public/pets", {
      cache: "no-store",
      query: { species: species || undefined, page, size: PAGE_SIZE },
    });
    return { data, error: null };
  } catch (e) {
    return { data: null, error: errorMessage(e) };
  }
}

function hrefFor(species: Species | "", page = 0) {
  const params = new URLSearchParams();
  if (species) params.set("species", species);
  if (page > 0) params.set("page", String(page));
  const qs = params.toString();
  return `/${qs ? `?${qs}` : ""}#pets`;
}

export default async function HomePage({ searchParams }: HomeProps) {
  const params = await searchParams;
  const species: Species | "" = params.species === "DOG" || params.species === "CAT" ? params.species : "";
  const page = Math.max(0, Number.parseInt(params.page ?? "0", 10) || 0);
  const { data, error } = await loadPets(species, page);

  return (
    <>
      <section className="hero">
        <div className="container hero__inner">
          <div className="hero__content">
            <p className="hero__eyebrow">Trung tâm cứu hộ chó mèo</p>
            <h1 className="hero__title">Mỗi bé được cứu đều xứng đáng có một mái nhà</h1>
            <p className="hero__lead">
              Chúng tôi cứu hộ chó mèo bị bỏ rơi, chăm sóc đến khi khỏe mạnh rồi tìm cho các bé một gia đình yêu
              thương.
            </p>
            <div className="hero__actions">
              <LinkButton href="#pets" size="lg" icon={<HeartHandshake aria-hidden="true" />}>
                Tìm bé để nhận nuôi
              </LinkButton>
              <LinkButton href="/rescue" size="lg" variant="ghost" icon={<Siren aria-hidden="true" />}>
                Báo thú cần cứu hộ
              </LinkButton>
            </div>
          </div>
          <aside className="hero__panel" aria-label="Cam kết của trung tâm">
            <p className="hero__stat">
              <span className="hero__stat-value">{data ? data.totalItems : "—"}</span>
              <span className="hero__stat-label">bé đang chờ một mái nhà</span>
            </p>
            <ul className="hero__facts">
              <li className="hero__fact">
                <Stethoscope aria-hidden="true" />
                <span>Khám thú y, tiêm phòng và tẩy giun trước khi cho nhận nuôi</span>
              </li>
              <li className="hero__fact">
                <ShieldCheck aria-hidden="true" />
                <span>Đánh giá đủ 6 điều kiện về sức khỏe, tuổi và tính cách</span>
              </li>
              <li className="hero__fact">
                <HousePlus aria-hidden="true" />
                <span>Thăm hỏi định kỳ sau khi bé về nhà mới</span>
              </li>
            </ul>
          </aside>
        </div>
      </section>

      <section id="pets" className="container pet-list" aria-labelledby="pet-list-title">
        <div className="pet-list__header">
          <div>
            <p className="pet-list__eyebrow">Nhận nuôi</p>
            <h2 id="pet-list-title" className="pet-list__title">
              Đang chờ nhận nuôi
            </h2>
            {data && <p className="pet-list__count">{data.totalItems} bé đang tìm nhà</p>}
          </div>
          <nav className="pet-list__filters" aria-label="Lọc theo loài">
            {FILTERS.map((f) => (
              <Link
                key={f.label}
                href={hrefFor(f.value)}
                scroll={false}
                className={`pet-list__filter${species === f.value ? " pet-list__filter--active" : ""}`}
                aria-current={species === f.value ? "true" : undefined}
              >
                {f.label}
              </Link>
            ))}
          </nav>
        </div>

        {error && <Notice tone="error" title="Không tải được danh sách">{error}</Notice>}
        {data && data.items.length === 0 && (
          <EmptyState
            title="Hiện chưa có bé nào sẵn sàng nhận nuôi"
            description="Các bé đang được chăm sóc và khám thú y. Bạn quay lại sau ít ngày nhé."
          />
        )}
        {data && data.items.length > 0 && (
          <div className="pet-list__grid">
            {data.items.map((pet) => (
              <PetCard key={pet.id} pet={pet} />
            ))}
          </div>
        )}
        {data && data.totalPages > 1 && (
          <nav className="pet-list__pages" aria-label="Phân trang">
            {page > 0 && (
              <Link href={hrefFor(species, page - 1)} className="pet-list__page">
                Trang trước
              </Link>
            )}
            <span className="pet-list__page-info">
              Trang {page + 1}/{data.totalPages}
            </span>
            {page < data.totalPages - 1 && (
              <Link href={hrefFor(species, page + 1)} className="pet-list__page">
                Trang sau
              </Link>
            )}
          </nav>
        )}
      </section>

      <section className="container steps" aria-labelledby="steps-title">
        <p className="steps__eyebrow">4 bước</p>
        <h2 id="steps-title" className="steps__title">
          Quy trình nhận nuôi
        </h2>
        <ol className="steps__list">
          {STEPS.map(({ icon: Icon, title, text }, i) => (
            <li key={title} className="steps__item">
              <span className="steps__icon" aria-hidden="true">
                <Icon />
              </span>
              <span className="steps__number">Bước {i + 1}</span>
              <h3 className="steps__name">{title}</h3>
              <p className="steps__text">{text}</p>
            </li>
          ))}
        </ol>
        <div className="steps__rescue">
          <div>
            <h3 className="steps__rescue-title">Bạn thấy chó mèo bị bỏ rơi, bị thương?</h3>
            <p className="steps__rescue-text">Gửi báo cáo kèm vị trí và ảnh, không cần tài khoản. Đội cứu hộ sẽ liên hệ lại.</p>
          </div>
          <LinkButton href="/rescue" variant="accent" icon={<ArrowRight aria-hidden="true" />}>
            Báo cứu hộ ngay
          </LinkButton>
        </div>
      </section>
    </>
  );
}
