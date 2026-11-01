import type { PublicPet } from "@/types/api";
import "./pet-card.scss";

const SPECIES_LABEL = { DOG: "Chó", CAT: "Mèo" } as const;
const SEX_LABEL = { MALE: "Đực", FEMALE: "Cái", UNKNOWN: "Chưa rõ giới tính" } as const;

function ageLabel(birthDate: string | null): string | null {
  if (!birthDate) return null;
  const months = Math.floor((Date.now() - Date.parse(birthDate)) / (1000 * 60 * 60 * 24 * 30.4));
  if (months < 12) return `Khoảng ${Math.max(months, 1)} tháng tuổi`;
  return `Khoảng ${Math.floor(months / 12)} tuổi`;
}

export function PetCard({ pet }: { pet: PublicPet }) {
  const age = ageLabel(pet.estimatedBirthDate);
  return (
    <article className={`pet-card pet-card--${pet.species.toLowerCase()}`}>
      <div className="pet-card__media" aria-hidden="true">
        {pet.species === "DOG" ? "🐶" : "🐱"}
      </div>
      <div className="pet-card__body">
        <h3 className="pet-card__name">{pet.name ?? "Chưa đặt tên"}</h3>
        <span className="pet-card__code">{pet.code}</span>
        <ul className="pet-card__tags">
          <li className="pet-card__tag">{SPECIES_LABEL[pet.species]}</li>
          <li className="pet-card__tag">{SEX_LABEL[pet.sex]}</li>
          {age && <li className="pet-card__tag">{age}</li>}
          {pet.color && <li className="pet-card__tag">{pet.color}</li>}
        </ul>
        {pet.description && <p className="pet-card__description">{pet.description}</p>}
      </div>
    </article>
  );
}
