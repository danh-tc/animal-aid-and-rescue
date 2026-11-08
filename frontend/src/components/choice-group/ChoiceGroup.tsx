"use client";

import { useId } from "react";
import "./choice-group.scss";

export interface Choice<V extends string> {
  value: V;
  label: string;
  hint?: string;
}

interface ChoiceGroupProps<V extends string> {
  legend: string;
  choices: Choice<V>[];
  value: V | null;
  onChange: (value: V) => void;
  required?: boolean;
  error?: string | null;
  columns?: 2 | 3;
}

export function ChoiceGroup<V extends string>({
  legend,
  choices,
  value,
  onChange,
  required,
  error,
  columns = 3,
}: ChoiceGroupProps<V>) {
  const name = useId();
  const errorId = `${name}-error`;
  return (
    <fieldset className="choice-group" aria-describedby={error ? errorId : undefined}>
      <legend className="choice-group__legend">
        {legend}
        {required && (
          <span className="choice-group__required" aria-hidden="true">
            *
          </span>
        )}
      </legend>
      <div className={`choice-group__options choice-group__options--${columns}`}>
        {choices.map((c) => (
          <label
            key={c.value}
            className={`choice-group__option${value === c.value ? " choice-group__option--selected" : ""}`}
          >
            <input
              type="radio"
              className="choice-group__input"
              name={name}
              value={c.value}
              checked={value === c.value}
              required={required}
              onChange={() => onChange(c.value)}
            />
            <span className="choice-group__label">{c.label}</span>
            {c.hint && <span className="choice-group__hint">{c.hint}</span>}
          </label>
        ))}
      </div>
      {error && (
        <p id={errorId} className="choice-group__error" role="alert">
          {error}
        </p>
      )}
    </fieldset>
  );
}
