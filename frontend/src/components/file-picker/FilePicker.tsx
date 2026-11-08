"use client";

import { ImagePlus, X } from "lucide-react";
import { useId, useRef } from "react";
import "./file-picker.scss";

interface FilePickerProps {
  label: string;
  files: File[];
  onChange: (files: File[]) => void;
  accept?: string;
  max?: number;
  hint?: string;
}

const MAX_SIZE = 5 * 1024 * 1024;

export function FilePicker({
  label,
  files,
  onChange,
  accept = "image/jpeg,image/png,image/webp",
  max = 5,
  hint = "Ảnh JPG, PNG hoặc WEBP, tối đa 5 tệp, mỗi tệp không quá 5MB.",
}: FilePickerProps) {
  const id = useId();
  const input = useRef<HTMLInputElement>(null);
  const tooBig = files.filter((f) => f.size > MAX_SIZE);

  function add(list: FileList | null) {
    if (!list) return;
    onChange([...files, ...Array.from(list)].slice(0, max));
    if (input.current) input.current.value = "";
  }

  return (
    <div className="file-picker">
      <span className="file-picker__label" id={`${id}-label`}>
        {label}
      </span>
      <label htmlFor={id} className={`file-picker__drop${files.length >= max ? " file-picker__drop--full" : ""}`}>
        <ImagePlus aria-hidden="true" />
        <span>{files.length >= max ? `Đã chọn đủ ${max} tệp` : "Chọn tệp từ máy"}</span>
        <input
          ref={input}
          id={id}
          type="file"
          className="sr-only"
          accept={accept}
          multiple
          disabled={files.length >= max}
          aria-describedby={`${id}-hint`}
          onChange={(e) => add(e.target.files)}
        />
      </label>
      <p id={`${id}-hint`} className="file-picker__hint">
        {hint}
      </p>
      {files.length > 0 && (
        <ul className="file-picker__list" aria-labelledby={`${id}-label`}>
          {files.map((f, i) => (
            <li key={`${f.name}-${i}`} className={`file-picker__file${f.size > MAX_SIZE ? " file-picker__file--invalid" : ""}`}>
              <span className="file-picker__file-name">{f.name}</span>
              <span className="file-picker__file-size">{(f.size / 1024 / 1024).toFixed(1)} MB</span>
              <button
                type="button"
                className="file-picker__remove"
                aria-label={`Bỏ tệp ${f.name}`}
                onClick={() => onChange(files.filter((_, j) => j !== i))}
              >
                <X aria-hidden="true" />
              </button>
            </li>
          ))}
        </ul>
      )}
      {tooBig.length > 0 && (
        <p className="file-picker__error" role="alert">
          Có tệp lớn hơn 5MB, hãy bỏ tệp đó hoặc chọn tệp nhỏ hơn.
        </p>
      )}
    </div>
  );
}

export function filesValid(files: File[]): boolean {
  return files.every((f) => f.size <= MAX_SIZE);
}
