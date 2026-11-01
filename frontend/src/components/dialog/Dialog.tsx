"use client";

import { X } from "lucide-react";
import { type FormEvent, type ReactNode, useEffect, useRef, useState } from "react";
import { Button } from "@/components/button/Button";
import { Notice } from "@/components/notice/Notice";
import { errorMessage } from "@/lib/api";
import "./dialog.scss";

interface DialogProps {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: ReactNode;
  children?: ReactNode;
  /** Nhãn nút xác nhận. Có thì hộp thoại là một form. */
  submitLabel?: string;
  submitVariant?: "primary" | "danger" | "accent";
  /** Ném lỗi thì hộp thoại giữ nguyên và hiện thông báo lỗi. */
  onSubmit?: () => Promise<void> | void;
  size?: "md" | "lg";
}

/**
 * Hộp thoại dùng thẻ <dialog> gốc: tự khóa focus, Esc để đóng, có nút đóng rõ ràng.
 */
export function Dialog({
  open,
  onClose,
  title,
  description,
  children,
  submitLabel,
  submitVariant = "primary",
  onSubmit,
  size = "md",
}: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    if (open && !el.open) {
      setError(null);
      el.showModal();
    } else if (!open && el.open) {
      el.close();
    }
  }, [open]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!onSubmit) return;
    setBusy(true);
    setError(null);
    try {
      await onSubmit();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <dialog
      ref={ref}
      className={`dialog dialog--${size}`}
      aria-labelledby="dialog-title"
      onCancel={(e) => {
        e.preventDefault();
        if (!busy) onClose();
      }}
      onClick={(e) => {
        // Bấm ra ngoài vùng nội dung để đóng
        if (e.target === ref.current && !busy) onClose();
      }}
    >
      <form className="dialog__panel" onSubmit={handleSubmit} noValidate={false}>
        <header className="dialog__header">
          <div>
            <h2 id="dialog-title" className="dialog__title">
              {title}
            </h2>
            {description && <p className="dialog__description">{description}</p>}
          </div>
          <button type="button" className="dialog__close" onClick={onClose} aria-label="Đóng" disabled={busy}>
            <X aria-hidden="true" />
          </button>
        </header>
        <div className="dialog__body">
          {error && <Notice tone="error">{error}</Notice>}
          {children}
        </div>
        {submitLabel && (
          <footer className="dialog__footer">
            <Button variant="ghost" onClick={onClose} disabled={busy}>
              Hủy
            </Button>
            <Button type="submit" variant={submitVariant} loading={busy}>
              {submitLabel}
            </Button>
          </footer>
        )}
      </form>
    </dialog>
  );
}
