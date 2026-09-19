import type { ReactNode } from 'react';
import type { ErrorDto } from '../api/types';

export function Card({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="card">
      <h2>{title}</h2>
      {children}
    </section>
  );
}

/** Ряд с переносом — для полей формы и кнопок. */
export function Row({ children }: { children: ReactNode }) {
  return <div className="row">{children}</div>;
}

interface FieldProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
  wide?: boolean;
  placeholder?: string;
  multiline?: boolean;
}

export function Field({ label, value, onChange, wide, placeholder, multiline }: FieldProps) {
  return (
    <label className={wide ? 'field field-wide' : 'field'}>
      <span className="field-label">{label}</span>
      {multiline ? (
        <textarea value={value} placeholder={placeholder} rows={3} onChange={(e) => onChange(e.target.value)} />
      ) : (
        <input type="text" value={value} placeholder={placeholder} onChange={(e) => onChange(e.target.value)} />
      )}
    </label>
  );
}

interface SelectFieldProps {
  label: string;
  value: string;
  options: readonly string[];
  onChange: (value: string) => void;
}

export function SelectField({ label, value, options, onChange }: SelectFieldProps) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      <select value={value} onChange={(e) => onChange(e.target.value)}>
        {options.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </select>
    </label>
  );
}

export function CheckField({ label, checked, onChange }: { label: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <label className="field field-check">
      <span className="field-label">{label}</span>
      <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} />
    </label>
  );
}

export function Btn({ children, primary, onClick }: { children: ReactNode; primary?: boolean; onClick: () => void }) {
  return (
    <button type="button" className={primary ? 'btn btn-primary' : 'btn'} onClick={onClick}>
      {children}
    </button>
  );
}

export function Chip({ children, active, onClick }: { children: ReactNode; active: boolean; onClick: () => void }) {
  return (
    <button type="button" className={active ? 'chip chip-on' : 'chip'} onClick={onClick}>
      {children}
    </button>
  );
}

export function Hint({ children }: { children: ReactNode }) {
  return <p className="hint">{children}</p>;
}

/** Абзац текста — один из видов человеко-читаемого представления, названных в задании. */
export function Paragraph({ children }: { children: ReactNode }) {
  return <p className="para">{children}</p>;
}

/** Таблица — второй вид представления из задания. */
export function DataTable({ headers, rows }: { headers: string[]; rows: string[][] }) {
  return (
    <table className="table">
      <thead>
        <tr>
          {headers.map((h) => (
            <th key={h}>{h}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row, i) => (
          <tr key={i}>
            {row.map((cell, j) => (
              <td key={j}>{cell}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

const TITLES: Record<number, string> = {
  0: 'Нет связи с сервисом',
  400: 'Некорректный запрос (400)',
  404: 'Не найдено (404)',
  409: 'Конфликт (409)',
  422: 'Данные нарушают ограничения (422)',
  503: 'Сервис недоступен (503)',
};

/**
 * Сообщение об ошибке сервиса.
 *
 * Показывает код, текст и перечень нарушенных ограничений из `details` — именно этого
 * требует задание: пользователь должен понять, что данные невалидны и какие именно.
 */
export function ErrorBanner({ error }: { error: ErrorDto }) {
  const title = TITLES[error.code] ?? `Ошибка ${error.code}`;
  const details = error.details ?? [];
  return (
    <div className={`banner banner-${error.code}`} role="alert">
      <div className="banner-title">{title}</div>
      <div>{error.message}</div>
      {details.length > 0 && (
        <>
          <div className="banner-sub">Нарушенные ограничения:</div>
          <ul>
            {details.map((d) => (
              <li key={d}>{d}</li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}

export function SuccessBanner({ children }: { children: ReactNode }) {
  return (
    <div className="banner banner-ok" role="status">
      {children}
    </div>
  );
}
