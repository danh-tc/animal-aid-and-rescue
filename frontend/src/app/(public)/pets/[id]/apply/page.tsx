"use client";

import { HeartHandshake } from "lucide-react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import { Button } from "@/components/button/Button";
import { ChoiceGroup } from "@/components/choice-group/ChoiceGroup";
import { Field } from "@/components/form-field/Field";
import { FormCard } from "@/components/form-card/FormCard";
import { Notice } from "@/components/notice/Notice";
import { RequireRole } from "@/components/require-role/RequireRole";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { useToast } from "@/components/toast/Toast";
import { useApi, useMutation } from "@/hooks/useApi";
import { ApiRequestError, errorMessage } from "@/lib/api";
import { petName } from "@/lib/format";
import type { ApplicationView, HousingType, MeView, PublicPet } from "@/types/api";

const HOUSING_CHOICES: { value: HousingType; label: string }[] = [
  { value: "HOUSE", label: "Nhà riêng" },
  { value: "APARTMENT", label: "Chung cư" },
  { value: "OTHER", label: "Khác" },
];

function ApplyForm() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const toast = useToast();
  const mutate = useMutation();
  const { data: pet, error: loadError, loading } = useApi<PublicPet>(`/api/public/pets/${id}`);

  const { data: me } = useApi<MeView>("/api/me");
  const [phone, setPhone] = useState("");
  const [address, setAddress] = useState("");

  const [housing, setHousing] = useState<HousingType | null>(null);
  const [experience, setExperience] = useState<"yes" | "no" | null>(null);
  const [motivation, setMotivation] = useState("");
  const [touched, setTouched] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if ((loading && !pet) || !me) return <Skeleton card lines={6} />;
  if (!pet) {
    return (
      <Notice tone="error" title="Không mở được đơn" action={<Link href="/">Xem các bé khác</Link>}>
        {loadError ?? "Bé này hiện không còn nhận đơn nhận nuôi."}
      </Notice>
    );
  }

  const name = petName(pet.name, pet.code);
  const needPhone = !me.phone;
  const needAddress = !me.address;
  const phoneValid = /^[0-9+]{9,15}$/.test(phone.trim());
  const errors = {
    phone: touched && needPhone && !phoneValid ? "Nhập số điện thoại 9 đến 15 chữ số để nhân viên liên hệ." : null,
    address: touched && needAddress && !address.trim() ? "Nhập địa chỉ nơi bé sẽ sống." : null,
    housing: touched && !housing ? "Chọn loại nhà ở hiện tại." : null,
    experience: touched && !experience ? "Cho biết bạn đã từng nuôi chó mèo chưa." : null,
  };

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setTouched(true);
    if (!housing || !experience || !pet) return;
    if ((needPhone && !phoneValid) || (needAddress && !address.trim())) return;
    setSubmitting(true);
    setError(null);
    try {
      await mutate<ApplicationView>("/api/adopter/applications", {
        body: {
          petId: pet.id,
          housingType: housing,
          hasPetExperience: experience === "yes",
          motivation: motivation.trim() || null,
          phone: needPhone ? phone.trim() : null,
          address: needAddress ? address.trim() : null,
        },
      });
      toast.success(`Đã nộp đơn nhận nuôi ${name}. Nhân viên sẽ liên hệ với bạn.`);
      router.push("/me/applications");
    } catch (e) {
      setError(
        e instanceof ApiRequestError && e.code === "DUPLICATE_APPLICATION"
          ? "Bạn đã có một đơn đang chờ cho bé này. Xem trong mục Đơn của tôi."
          : errorMessage(e),
      );
      setSubmitting(false);
    }
  }

  return (
    <FormCard
      wide
      title={`Nộp đơn nhận nuôi ${name}`}
      icon={HeartHandshake}
      description={
        <>
          Mã hồ sơ {pet.code}. <Link href={`/pets/${pet.id}`}>Xem lại hồ sơ của bé</Link>
        </>
      }
    >
      <form className="stack stack--lg" onSubmit={handleSubmit} noValidate>
        {error && <Notice tone="error">{error}</Notice>}
        {(needPhone || needAddress) && (
          <div className="stack">
            <Notice tone="info">
              Tài khoản của bạn chưa có {needPhone && needAddress ? "số điện thoại và địa chỉ" : needPhone ? "số điện thoại" : "địa chỉ"}.
              Nhân viên cần thông tin này để liên hệ khi xét duyệt và hậu kiểm. Hệ thống sẽ lưu lại cho các lần nộp đơn sau.
            </Notice>
            {needPhone && (
              <Field label="Số điện thoại" required error={errors.phone}>
                <input type="tel" inputMode="tel" autoComplete="tel" value={phone} onChange={(e) => setPhone(e.target.value)} />
              </Field>
            )}
            {needAddress && (
              <Field label="Địa chỉ" required hint="Nơi bé sẽ sống, dùng cho lần hậu kiểm thăm nhà." error={errors.address}>
                <input autoComplete="street-address" maxLength={255} value={address} onChange={(e) => setAddress(e.target.value)} />
              </Field>
            )}
          </div>
        )}
        <ChoiceGroup
          legend="Bạn đang ở"
          required
          choices={HOUSING_CHOICES}
          value={housing}
          onChange={setHousing}
          error={errors.housing}
        />
        <ChoiceGroup
          legend="Bạn đã từng nuôi chó mèo chưa?"
          required
          columns={2}
          choices={[
            { value: "yes", label: "Đã từng nuôi" },
            { value: "no", label: "Chưa từng nuôi" },
          ]}
          value={experience}
          onChange={setExperience}
          error={errors.experience}
        />
        <Field label="Vì sao bạn muốn nhận nuôi bé?" hint="Không bắt buộc. Chia sẻ về gia đình, thời gian chăm sóc, không gian sống.">
          <textarea rows={4} maxLength={2000} value={motivation} onChange={(e) => setMotivation(e.target.value)} />
        </Field>
        <Button type="submit" size="lg" block loading={submitting}>
          Gửi đơn nhận nuôi
        </Button>
      </form>
    </FormCard>
  );
}

export default function ApplyPage() {
  return (
    <div className="container">
      <RequireRole roles={["ADOPTER"]}>
        <ApplyForm />
      </RequireRole>
    </div>
  );
}
