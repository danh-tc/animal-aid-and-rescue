import type { ReactNode } from "react";
import "./toolbar.scss";

/** Hàng công cụ phía trên danh sách: bộ lọc bên trái, tìm kiếm hoặc thao tác bên phải. */
export function Toolbar({ children, end }: { children?: ReactNode; end?: ReactNode }) {
  return (
    <div className="toolbar">
      <div className="toolbar__start">{children}</div>
      {end && <div className="toolbar__end">{end}</div>}
    </div>
  );
}
