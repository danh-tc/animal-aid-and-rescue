import Link from "next/link";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { Spinner } from "@/components/spinner/Spinner";
import "./button.scss";

type Variant = "primary" | "accent" | "secondary" | "ghost" | "danger";
type Size = "md" | "sm" | "lg";

interface Common {
  variant?: Variant;
  size?: Size;
  block?: boolean;
  icon?: ReactNode;
  className?: string;
  children?: ReactNode;
}

export function buttonClass({ variant = "primary", size = "md", block, className }: Common): string {
  return [
    "btn",
    `btn--${variant}`,
    size !== "md" ? `btn--${size}` : "",
    block ? "btn--block" : "",
    className ?? "",
  ]
    .filter(Boolean)
    .join(" ");
}

interface ButtonProps extends Common, Omit<ButtonHTMLAttributes<HTMLButtonElement>, "className" | "children"> {

  loading?: boolean;
}

export function Button({ variant, size, block, icon, className, children, loading, disabled, ...rest }: ButtonProps) {
  return (
    <button
      type="button"
      {...rest}
      className={buttonClass({ variant, size, block, className })}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
    >
      {loading ? <Spinner /> : icon}
      {children && <span className="btn__label">{children}</span>}
    </button>
  );
}

interface LinkButtonProps extends Common {
  href: string;
}

export function LinkButton({ href, variant, size, block, icon, className, children }: LinkButtonProps) {
  return (
    <Link href={href} className={buttonClass({ variant, size, block, className })}>
      {icon}
      {children && <span className="btn__label">{children}</span>}
    </Link>
  );
}
