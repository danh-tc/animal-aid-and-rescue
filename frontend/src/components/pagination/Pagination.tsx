import { ChevronLeft, ChevronRight } from "lucide-react";
import "./pagination.scss";

interface PaginationProps {
  page: number;
  totalPages: number;
  totalItems: number;
  onChange: (page: number) => void;
}

export function Pagination({ page, totalPages, totalItems, onChange }: PaginationProps) {
  if (totalPages <= 1) return null;
  return (
    <nav className="pagination" aria-label="Phân trang">
      <span className="pagination__info">
        Trang {page + 1}/{totalPages} · {totalItems} mục
      </span>
      <div className="pagination__controls">
        <button
          type="button"
          className="pagination__button"
          onClick={() => onChange(page - 1)}
          disabled={page <= 0}
          aria-label="Trang trước"
        >
          <ChevronLeft aria-hidden="true" />
        </button>
        <button
          type="button"
          className="pagination__button"
          onClick={() => onChange(page + 1)}
          disabled={page >= totalPages - 1}
          aria-label="Trang sau"
        >
          <ChevronRight aria-hidden="true" />
        </button>
      </div>
    </nav>
  );
}
