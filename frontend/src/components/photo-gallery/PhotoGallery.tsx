"use client";

import { Cat, Dog } from "lucide-react";
import { useState } from "react";
import { publicPhotoUrl } from "@/lib/api";
import type { Species } from "@/types/api";
import "./photo-gallery.scss";

interface PhotoGalleryProps {
  petId: number;
  photoIds: number[];
  species: Species;
  name: string;
}

export function PhotoGallery({ petId, photoIds, species, name }: PhotoGalleryProps) {
  const [index, setIndex] = useState(0);
  const Fallback = species === "DOG" ? Dog : Cat;

  if (photoIds.length === 0) {
    return (
      <div className={`photo-gallery photo-gallery--empty photo-gallery--${species.toLowerCase()}`}>
        <div className="photo-gallery__main">
          <Fallback className="photo-gallery__fallback" aria-hidden="true" />
          <span className="sr-only">Chưa có ảnh của {name}</span>
        </div>
      </div>
    );
  }

  return (
    <div className="photo-gallery">
      <div className="photo-gallery__main">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={publicPhotoUrl(petId, photoIds[index])}
          alt={`Ảnh ${index + 1} của ${name}`}
          className="photo-gallery__image"
          width={800}
          height={600}
        />
      </div>
      {photoIds.length > 1 && (
        <div className="photo-gallery__thumbs" role="group" aria-label="Chọn ảnh">
          {photoIds.map((id, i) => (
            <button
              key={id}
              type="button"
              className={`photo-gallery__thumb${i === index ? " photo-gallery__thumb--active" : ""}`}
              aria-pressed={i === index}
              aria-label={`Xem ảnh ${i + 1}`}
              onClick={() => setIndex(i)}
            >
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={publicPhotoUrl(petId, id)} alt="" width={80} height={80} loading="lazy" />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
