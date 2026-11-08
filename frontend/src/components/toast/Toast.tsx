"use client";

import { CheckCircle2, X, XCircle } from "lucide-react";
import { createContext, type ReactNode, useCallback, useContext, useMemo, useState } from "react";
import "./toast.scss";

type ToastTone = "success" | "error";
interface ToastItem {
  id: number;
  tone: ToastTone;
  message: string;
}

interface ToastApi {
  success: (message: string) => void;
  error: (message: string) => void;
}

const ToastContext = createContext<ToastApi | null>(null);
let nextId = 1;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);

  const dismiss = useCallback((id: number) => setItems((list) => list.filter((t) => t.id !== id)), []);

  const push = useCallback(
    (tone: ToastTone, message: string) => {
      const id = nextId++;
      setItems((list) => [...list.slice(-2), { id, tone, message }]);
      window.setTimeout(() => dismiss(id), tone === "error" ? 6000 : 4000);
    },
    [dismiss],
  );

  const api = useMemo<ToastApi>(
    () => ({ success: (m) => push("success", m), error: (m) => push("error", m) }),
    [push],
  );

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toast" aria-live="polite" aria-atomic="false">
        {items.map((t) => {
          const Icon = t.tone === "success" ? CheckCircle2 : XCircle;
          return (
            <div key={t.id} className={`toast__item toast__item--${t.tone}`}>
              <Icon className="toast__icon" aria-hidden="true" />
              <p className="toast__message">{t.message}</p>
              <button type="button" className="toast__close" onClick={() => dismiss(t.id)} aria-label="Đóng thông báo">
                <X aria-hidden="true" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast phải nằm trong ToastProvider");
  return ctx;
}
