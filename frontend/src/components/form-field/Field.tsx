import { type ReactElement, type ReactNode, cloneElement, isValidElement, useId } from "react";
import "./form-field.scss";

interface FieldProps {
  label: string;
  /** Gợi ý luôn hiển thị dưới ô nhập, không thay cho nhãn. */
  hint?: ReactNode;
  error?: string | null;
  required?: boolean;
  className?: string;
  /** Một ô nhập duy nhất, Field tự gắn id, aria-describedby, aria-invalid. */
  children: ReactElement<Record<string, unknown>>;
}

export function Field({ label, hint, error, required, className, children }: FieldProps) {
  const autoId = useId();
  const id = (isValidElement(children) && (children.props.id as string)) || autoId;
  const hintId = hint ? `${id}-hint` : undefined;
  const errorId = error ? `${id}-error` : undefined;
  const describedBy = [hintId, errorId].filter(Boolean).join(" ") || undefined;

  return (
    <div className={`form-field${error ? " form-field--invalid" : ""}${className ? ` ${className}` : ""}`}>
      <label className="form-field__label" htmlFor={id}>
        {label}
        {required && (
          <span className="form-field__required" aria-hidden="true">
            *
          </span>
        )}
      </label>
      {cloneElement(children, {
        id,
        required,
        "aria-describedby": describedBy,
        "aria-invalid": error ? true : undefined,
        className: `form-field__control ${(children.props.className as string) ?? ""}`.trim(),
      })}
      {hint && (
        <p id={hintId} className="form-field__hint">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="form-field__error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}

interface CheckProps {
  label: ReactNode;
  checked: boolean;
  onChange: (checked: boolean) => void;
  hint?: ReactNode;
  disabled?: boolean;
  name?: string;
}

/** Ô đánh dấu có vùng bấm lớn (cả dòng), dùng cho checklist và cam kết. */
export function CheckField({ label, checked, onChange, hint, disabled, name }: CheckProps) {
  const id = useId();
  return (
    <label className={`form-field form-field--check${checked ? " form-field--checked" : ""}`} htmlFor={id}>
      <input
        id={id}
        name={name}
        type="checkbox"
        className="form-field__checkbox"
        checked={checked}
        disabled={disabled}
        onChange={(e) => onChange(e.target.checked)}
      />
      <span className="form-field__check-text">
        <span className="form-field__check-label">{label}</span>
        {hint && <span className="form-field__hint">{hint}</span>}
      </span>
    </label>
  );
}
