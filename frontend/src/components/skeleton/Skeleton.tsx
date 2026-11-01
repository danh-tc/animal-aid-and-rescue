import "./skeleton.scss";

/** Khung chờ giữ chỗ để tránh nhảy bố cục khi dữ liệu về. */
export function Skeleton({ lines = 3, card }: { lines?: number; card?: boolean }) {
  return (
    <div className={`skeleton${card ? " skeleton--card" : ""}`} aria-hidden="true">
      {Array.from({ length: lines }, (_, i) => (
        <span key={i} className="skeleton__line" />
      ))}
    </div>
  );
}
