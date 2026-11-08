"use client";

import type { LucideIcon } from "lucide-react";
import { type Column, DataTable } from "@/components/data-table/DataTable";
import { EmptyState } from "@/components/empty-state/EmptyState";
import { Notice } from "@/components/notice/Notice";
import { Pagination } from "@/components/pagination/Pagination";
import { Skeleton } from "@/components/skeleton/Skeleton";
import type { PageResponse } from "@/types/api";
import "./list-view.scss";

interface ListViewProps<T> {
  caption: string;
  data: PageResponse<T> | T[] | null;
  loading: boolean;
  error: string | null;
  columns: Column<T>[];
  rowKey: (row: T) => string | number;
  rowHref?: (row: T) => string;
  emptyTitle: string;
  emptyDescription?: string;
  emptyIcon?: LucideIcon;
  onPage?: (page: number) => void;
  onRetry?: () => void;
}

export function ListView<T>(props: ListViewProps<T>) {
  const { data, loading, error, onPage, onRetry } = props;
  const rows = Array.isArray(data) ? data : (data?.items ?? []);
  const paged = data && !Array.isArray(data) ? data : null;

  return (
    <div className="list-view">
      {error && (
        <div className="list-view__notice">
          <Notice
            tone="error"
            title="Không tải được dữ liệu"
            action={
              onRetry && (
                <button type="button" className="list-view__retry" onClick={onRetry}>
                  Thử lại
                </button>
              )
            }
          >
            {error}
          </Notice>
        </div>
      )}
      {loading && !data && <Skeleton lines={5} />}
      {data && rows.length === 0 && (
        <EmptyState icon={props.emptyIcon} title={props.emptyTitle} description={props.emptyDescription} />
      )}
      {rows.length > 0 && (
        <DataTable
          caption={props.caption}
          columns={props.columns}
          rows={rows}
          rowKey={props.rowKey}
          rowHref={props.rowHref}
          loading={loading}
        />
      )}
      {paged && onPage && (
        <Pagination page={paged.page} totalPages={paged.totalPages} totalItems={paged.totalItems} onChange={onPage} />
      )}
    </div>
  );
}
