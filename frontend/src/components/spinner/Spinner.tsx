import "./spinner.scss";

export function Spinner({ label }: { label?: string }) {
  return (
    <span className="spinner" role={label ? "status" : undefined} aria-hidden={label ? undefined : true}>
      {label && <span className="sr-only">{label}</span>}
    </span>
  );
}
