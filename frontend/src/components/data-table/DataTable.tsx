import Link from "next/link";
import type { ReactNode } from "react";
import "./data-table.scss";

export interface Column<T> {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
  /** Cột số: căn phải, chữ số đều nhau. */
  numeric?: boolean;
  /** Ẩn trên màn hình nhỏ để giữ thẻ gọn. */
  hideOnMobile?: boolean;
}

interface DataTableProps<T> {
  caption: string;
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string | number;
  /** Có href thì ô đầu tiên là liên kết, bấm cả dòng đều mở được. */
  rowHref?: (row: T) => string;
  loading?: boolean;
}

export function DataTable<T>({ caption, columns, rows, rowKey, rowHref, loading }: DataTableProps<T>) {
  return (
    <div className={`data-table${loading ? " data-table--loading" : ""}`} aria-busy={loading || undefined}>
      <table className="data-table__table">
        <caption className="sr-only">{caption}</caption>
        <thead>
          <tr>
            {columns.map((c) => (
              <th
                key={c.key}
                scope="col"
                className={`data-table__th${c.numeric ? " data-table__th--numeric" : ""}${c.hideOnMobile ? " data-table__th--optional" : ""}`}
              >
                {c.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const href = rowHref?.(row);
            return (
              <tr key={rowKey(row)} className={`data-table__row${href ? " data-table__row--link" : ""}`}>
                {columns.map((c, i) => (
                  <td
                    key={c.key}
                    data-label={c.header}
                    className={`data-table__td${c.numeric ? " data-table__td--numeric" : ""}${c.hideOnMobile ? " data-table__td--optional" : ""}${i === 0 ? " data-table__td--primary" : ""}`}
                  >
                    {i === 0 && href ? (
                      <Link href={href} className="data-table__link">
                        {c.render(row)}
                      </Link>
                    ) : (
                      c.render(row)
                    )}
                  </td>
                ))}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
