"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useCallback } from "react";

/**
 * Lưu bộ lọc trên URL để quay lại trang vẫn giữ trạng thái và chia sẻ được đường dẫn.
 * Trang dùng hook này phải nằm trong <Suspense>.
 */
export function useUrlState<T extends string>(key: string, fallback: T): [T, (value: T) => void] {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const value = (params.get(key) as T | null) ?? fallback;

  const set = useCallback(
    (next: T) => {
      const search = new URLSearchParams(params.toString());
      if (next === fallback || next === "") search.delete(key);
      else search.set(key, next);
      // Đổi bộ lọc thì về trang đầu
      if (key !== "page") search.delete("page");
      const qs = search.toString();
      router.replace(`${pathname}${qs ? `?${qs}` : ""}`, { scroll: false });
    },
    [params, router, pathname, key, fallback],
  );

  return [value, set];
}

export function usePageParam(): [number, (page: number) => void] {
  const [raw, setRaw] = useUrlState<string>("page", "0");
  const page = Math.max(0, Number.parseInt(raw, 10) || 0);
  return [page, (p: number) => setRaw(String(p))];
}
