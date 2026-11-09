import { Cat, Dog } from "lucide-react";
import Link from "next/link";
import { publicPhotoUrl } from "@/lib/api";
import { ageLabel, petName } from "@/lib/format";
import { SPECIES } from "@/lib/labels";
import type { PublicPet } from "@/types/api";
import "./pet-card.scss";

export function PetCard({ pet }: { pet: PublicPet }) {
  const age = ageLabel(pet.estimatedBirthDate);
  const name = petName(pet.name);
  const Fallback = pet.species === "DOG" ? Dog : Cat;
  const photo = pet.photoIds[0];

  return (
    <article className={`pet-card pet-card--${pet.species.toLowerCase()}`}>
      <div className="pet-card__media">
        {photo ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            className="pet-card__image"
            src={publicPhotoUrl(pet.id, photo)}
            alt={`Ảnh của ${name}`}
            width={400}
            height={300}
            loading="lazy"
          />
        ) : (
          <Fallback className="pet-card__fallback" aria-hidden="true" />
        )}
        <span className="pet-card__species">{SPECIES[pet.species]}</span>
      </div>
      <div className="pet-card__body">
        <h3 className="pet-card__name">
          <Link href={`/pets/${pet.id}`} className="pet-card__link">
            {name}
          </Link>
        </h3>
        <ul className="pet-card__tags">
          {pet.sex !== "UNKNOWN" && (
<li className="pet-card__tag">{pet.sex === "MALE" ? "Đực" : "Cái"}</li>
          )}
          {age && <li className="pet-card__tag">{age}</li>}
          {pet.color && <li className="pet-card__tag">{pet.color}</li>}
        </ul>
        {pet.description && <p className="pet-card__description">{pet.description}</p>}
        <span className="pet-card__more" aria-hidden="true">
          Xem chi tiết
        </span>
      </div>
    </article>
  );
}
