"use client";

import { Eye, EyeOff } from "lucide-react";
import { type InputHTMLAttributes, useState } from "react";
import "./password-input.scss";

type Props = Omit<InputHTMLAttributes<HTMLInputElement>, "type">;

/** Ô mật khẩu có nút hiện/ẩn, vẫn cho trình quản lý mật khẩu tự điền và dán. */
export function PasswordInput({ className, ...props }: Props) {
  const [visible, setVisible] = useState(false);
  return (
    <span className="password-input">
      <input {...props} type={visible ? "text" : "password"} className={`${className ?? ""} password-input__field`} />
      <button
        type="button"
        className="password-input__toggle"
        aria-label={visible ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
        aria-pressed={visible}
        onClick={() => setVisible((v) => !v)}
      >
        {visible ? <EyeOff aria-hidden="true" /> : <Eye aria-hidden="true" />}
      </button>
    </span>
  );
}
