import { useId } from 'react';

interface Props {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  hint?: string;
  /** Подсказка из уже существующих значений; ввод произвольного остаётся возможным. */
  options?: readonly string[];
  autoFocus?: boolean;
}

export function TextField({ label, value, onChange, placeholder, hint, options, autoFocus }: Props) {
  const listId = useId();
  return (
    <fieldset className="fieldset">
      <legend className="fieldset-legend">{label}</legend>
      <input
        type="text"
        className="input input-sm w-full"
        value={value}
        placeholder={placeholder}
        list={options ? listId : undefined}
        autoFocus={autoFocus}
        onChange={(event) => onChange(event.target.value)}
      />
      {options && (
        <datalist id={listId}>
          {options.map((option) => (
            <option key={option} value={option} />
          ))}
        </datalist>
      )}
      {hint && <p className="label">{hint}</p>}
    </fieldset>
  );
}
