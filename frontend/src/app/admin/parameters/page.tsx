"use client";

import { Settings2 } from "lucide-react";
import { type FormEvent, useState } from "react";
import { Button } from "@/components/button/Button";
import { EmptyState } from "@/components/empty-state/EmptyState";
import { Notice } from "@/components/notice/Notice";
import { PageHeader } from "@/components/page-header/PageHeader";
import { Skeleton } from "@/components/skeleton/Skeleton";
import { useToast } from "@/components/toast/Toast";
import { useApi, useMutation } from "@/hooks/useApi";
import { errorMessage } from "@/lib/api";
import type { ParameterView } from "@/types/api";
import "./parameter-list.scss";

/** M04: Tham số hệ thống */
export default function ParametersPage() {
  const { data, error, loading, reload } = useApi<ParameterView[]>("/api/admin/system-parameters");

  return (
    <>
      <PageHeader
        title="Tham số hệ thống"
        description="Ngưỡng dùng trong checklist, hậu kiểm và giới hạn báo cáo. Giá trị là số nguyên dương."
      />
      {error && <Notice tone="error">{error}</Notice>}
      {loading && !data && <Skeleton card lines={6} />}
      {data?.length === 0 && <EmptyState icon={Settings2} title="Chưa có tham số" />}
      {data && data.length > 0 && (
        <ul className="parameter-list">
          {data.map((p) => (
            <ParameterRow key={p.key} param={p} onSaved={reload} />
          ))}
        </ul>
      )}
    </>
  );
}

function ParameterRow({ param, onSaved }: { param: ParameterView; onSaved: () => void }) {
  const mutate = useMutation();
  const toast = useToast();
  const [value, setValue] = useState(param.value);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const dirty = value !== param.value;
  const inputId = `param-${param.key}`;

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!/^\d+$/.test(value) || Number(value) < 1 || Number(value) > 10000) {
      setError("Nhập số nguyên từ 1 đến 10000.");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await mutate(`/api/admin/system-parameters/${param.key}`, { method: "PUT", body: { value } });
      toast.success(`Đã lưu ${param.key}.`);
      onSaved();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <li className="parameter-list__item">
      <form className="parameter-list__form" onSubmit={save}>
        <div className="parameter-list__text">
          <label htmlFor={inputId} className="parameter-list__key">
            {param.key}
          </label>
          {param.description && <p className="parameter-list__description">{param.description}</p>}
          {error && (
            <p className="parameter-list__error" role="alert">
              {error}
            </p>
          )}
        </div>
        <div className="parameter-list__controls">
          <input
            id={inputId}
            className="parameter-list__input"
            type="number"
            inputMode="numeric"
            min={1}
            max={10000}
            value={value}
            aria-invalid={error ? true : undefined}
            onChange={(e) => setValue(e.target.value)}
          />
          <Button type="submit" size="sm" variant={dirty ? "primary" : "ghost"} disabled={!dirty} loading={saving}>
            Lưu
          </Button>
        </div>
      </form>
    </li>
  );
}
