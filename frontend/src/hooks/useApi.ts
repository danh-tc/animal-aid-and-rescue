"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { type ApiOptions, apiFetch, errorMessage } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";

interface ApiQuery<T> {
  data: T | null;
  error: string | null;
  loading: boolean;
  reload: () => void;
}

/**
 * Đọc dữ liệu từ API kèm JWT hiện tại. path = null thì chưa gọi.
 * Giữ dữ liệu cũ khi tải lại để tránh nhảy bố cục.
 */
export function useApi<T>(path: string | null, query?: ApiOptions["query"]): ApiQuery<T> {
  const { token, ready } = useAuth();
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [nonce, setNonce] = useState(0);
  const queryKey = JSON.stringify(query ?? {});
  const seq = useRef(0);

  useEffect(() => {
    if (!ready || path === null) return;
    const current = ++seq.current;
    setLoading(true);
    apiFetch<T>(path, { token, query: JSON.parse(queryKey) as ApiOptions["query"], cache: "no-store" })
      .then((result) => {
        if (current !== seq.current) return;
        setData(result);
        setError(null);
      })
      .catch((e: unknown) => {
        if (current !== seq.current) return;
        setError(errorMessage(e));
      })
      .finally(() => {
        if (current === seq.current) setLoading(false);
      });
  }, [path, queryKey, token, ready, nonce]);

  const reload = useCallback(() => setNonce((n) => n + 1), []);
  return { data, error, loading, reload };
}

/** Gọi API thay đổi dữ liệu, trả về hàm gọi có JWT sẵn. */
export function useMutation() {
  const { token } = useAuth();
  return useCallback(
    <T,>(path: string, options: Omit<ApiOptions, "token"> = {}) =>
      apiFetch<T>(path, { method: "POST", ...options, token }),
    [token],
  );
}
