"use client";

import { HeartHandshake, LogIn } from "lucide-react";
import { LinkButton } from "@/components/button/Button";
import { isStaff, useAuth } from "@/lib/auth-context";
import "./adopt-cta.scss";

export function AdoptCta({ petId, petName }: { petId: number; petName: string }) {
  const { user, ready } = useAuth();
  const applyHref = `/pets/${petId}/apply`;

  return (
    <div className="adopt-cta">
      <div className="adopt-cta__text">
        <p className="adopt-cta__title">Đưa {petName} về nhà</p>
        <p className="adopt-cta__hint">
          Nộp đơn mất khoảng 3 phút. Nhân viên sẽ liên hệ để trao đổi trước khi duyệt.
        </p>
      </div>
      {!ready ? null : !user ? (
        <LinkButton href={`/login?next=${encodeURIComponent(applyHref)}`} size="lg" icon={<LogIn aria-hidden="true" />}>
          Đăng nhập để nộp đơn
        </LinkButton>
      ) : isStaff(user.role) ? (
        <LinkButton href={`/staff/pets/${petId}`} size="lg" variant="secondary">
          Mở hồ sơ nội bộ
        </LinkButton>
      ) : (
        <LinkButton href={applyHref} size="lg" icon={<HeartHandshake aria-hidden="true" />}>
          Nộp đơn nhận nuôi
        </LinkButton>
      )}
    </div>
  );
}
