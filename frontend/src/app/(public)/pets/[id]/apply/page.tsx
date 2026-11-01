"use client";

import { HeartHandshake } from "lucide-react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";
import { Button } from "@/components/button/Button";
import { ChoiceGroup } from "@/components/choice-group/ChoiceGroup";
import { CheckField, Field } from "@/components/form-field/Field";
import { FormCard } from "@/components/form-card/FormCard";
import { Notice } from "@/components/notice/Notice";
import { RequireRole } from "@/components/require-role/RequireRole";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { useToast } from "@/components/toast/Toast";
import { useApi, useMutation } from "@/hooks/useApi";
import { ApiRequestError, errorMessage } from "@/lib/api";
import { petName } from "@/lib/format";
import type { ApplicationView, HousingType, PublicPet } from "@/types/api";

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

  const [housing, setHousing] = useState<HousingType | null>(null);
  const [experience, setExperience] = useState<"yes" | "no" | null>(null);
  const [motivation, setMotivation] = useState("");
  const [pledge, setPledge] = useState(false);
  const [touched, setTouched] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (loading && !pet) return <Skeleton card lines={6} />;
  if (!pet) {
    return (
      <Notice tone="error" title="Không mở được đơn" action={<Link href="/">Xem các bé khác</Link>}>
        {loadError ?? "Bé này không còn mở nhận nuôi."}
      </Notice>
    );
  }

  const name = petName(pet.name, pet.code);
  const needPledge = !pet.sterilized;
  const errors = {
    housing: touched && !housing ? "Chọn loại nhà ở hiện tại." : null,
    experience: touched && !experience ? "Cho biết bạn đã từng nuôi chó mèo chưa." : null,
    pledge: touched && needPledge && !pledge ? "Bé chưa triệt sản, bạn cần cam kết để nộp đơn." : null,
  };

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setTouched(true);
    if (!housing || !experience || (needPledge && !pledge) || !pet) return;
    setSubmitting(true);
    setError(null);
    try {
      await mutate<ApplicationView>("/api/adopter/applications", {
        body: {
          petId: pet.id,
          housingType: housing,
          hasPetExperience: experience === "yes",
          motivation: motivation.trim() || null,
          sterilizationPledge: needPledge ? pledge : null,
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
        {needPledge && (
          <div className="stack stack--sm">
            <CheckField
              checked={pledge}
              onChange={setPledge}
              label="Tôi cam kết đưa bé đi triệt sản khi bé đủ tuổi"
              hint="Bé chưa triệt sản vì chưa đủ tuổi. Trung tâm sẽ nhắc lại trong các lần hậu kiểm."
            />
            {errors.pledge && (
              <p className="form-field__error" role="alert">
                {errors.pledge}
              </p>
            )}
          </div>
        )}
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
