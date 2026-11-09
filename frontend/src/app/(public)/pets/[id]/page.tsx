import { CalendarHeart, Palette, ShieldCheck, Sparkles, Tag } from "lucide-react";
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { AdoptCta } from "@/components/adopt-cta/AdoptCta";
import { Notice } from "@/components/notice/Notice";
import { PageHeader } from "@/components/page-header/PageHeader";
import { PhotoGallery } from "@/components/photo-gallery/PhotoGallery";
import { ApiRequestError, apiFetch, errorMessage } from "@/lib/api";
import { ageLabel, petName } from "@/lib/format";
import { SEX, SPECIES } from "@/lib/labels";
import type { PublicPet } from "@/types/api";
import "./pet-profile.scss";

export const dynamic = "force-dynamic";

interface Props {
  params: Promise<{ id: string }>;
}

async function load(id: string): Promise<{ pet: PublicPet | null; error: string | null }> {
  if (!/^\d+$/.test(id)) notFound();
  try {
    return { pet: await apiFetch<PublicPet>(`/api/public/pets/${id}`, { cache: "no-store" }), error: null };
  } catch (e) {

    if (e instanceof ApiRequestError && e.status === 404) notFound();
    return { pet: null, error: errorMessage(e) };
  }
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  const { pet } = await load(id);
  return { title: pet ? petName(pet.name, pet.code) : "Chi tiết thú" };
}

export default async function PetDetailPage({ params }: Props) {
  const { id } = await params;
  const { pet, error } = await load(id);

  if (!pet) {
    return (
      <div className="container pet-profile">
        <Notice tone="error" title="Không tải được hồ sơ">
          {error}
        </Notice>
      </div>
    );
  }

  const name = petName(pet.name, pet.code);
  const age = ageLabel(pet.estimatedBirthDate);
  const facts = [
    { icon: Tag, label: "Loài", value: SPECIES[pet.species] },
    { icon: Sparkles, label: "Giới tính", value: SEX[pet.sex] },
    { icon: CalendarHeart, label: "Tuổi", value: age ?? "Chưa rõ" },
    { icon: Palette, label: "Màu lông", value: pet.color ?? "Chưa rõ" },
  ];

  return (
    <div className="container pet-profile">
      <PageHeader back={{ href: "/#pets", label: "Tất cả các bé" }} title={name} meta={<span>Mã hồ sơ {pet.code}</span>} />

      <div className="pet-profile__layout">
        <PhotoGallery petId={pet.id} photoIds={pet.photoIds} species={pet.species} name={name} />

        <div className="pet-profile__info">
          <ul className="pet-profile__facts">
            {facts.map(({ icon: Icon, label, value }) => (
              <li key={label} className="pet-profile__fact">
                <Icon aria-hidden="true" />
                <span className="pet-profile__fact-label">{label}</span>
                <span className="pet-profile__fact-value">{value}</span>
              </li>
            ))}
          </ul>

          <section className="pet-profile__section" aria-labelledby="about-title">
            <h2 id="about-title" className="pet-profile__section-title">
              Về {name}
            </h2>
            <p className="pet-profile__description">
              {pet.description ?? "Bé đang chờ một gia đình yêu thương. Liên hệ trung tâm để biết thêm về tính cách của bé."}
            </p>
          </section>

          <section className="pet-profile__section" aria-labelledby="health-title">
            <h2 id="health-title" className="pet-profile__section-title">
              Sức khỏe
            </h2>
            <ul className="pet-profile__health">
              <li>
                <ShieldCheck aria-hidden="true" />
                Đã khám thú y, tiêm phòng cơ bản và tẩy giun
              </li>
              <li>
                <ShieldCheck aria-hidden="true" />
                Đã hết thời gian cách ly, tính cách hiền lành
              </li>
              <li>
                <ShieldCheck aria-hidden="true" />
                {pet.sterilized ? "Đã triệt sản" : "Chưa triệt sản do chưa đủ tuổi"}
              </li>
            </ul>
          </section>

          <AdoptCta petId={pet.id} petName={name} />
        </div>
      </div>
    </div>
  );
}
