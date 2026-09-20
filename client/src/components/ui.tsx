import { useId, type ReactNode } from 'react';
import type { ErrorDto } from '../api/types';
import { S } from '../strings';

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return (
    <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-2xl font-bold">{title}</h1>
        {subtitle && <p className="text-sm opacity-60">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  );
}

export function Panel({ title, children, className = '' }: { title?: string; children: ReactNode; className?: string }) {
  return (
    <section className={`card bg-base-100 shadow-sm ${className}`}>
      <div className="card-body gap-3">
        {title && <h2 className="card-title text-base">{title}</h2>}
        {children}
      </div>
    </section>
  );
}

interface TextFieldProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  hint?: string;
  /** Подсказка значений из уже существующих; ввод произвольного значения остаётся возможным. */
  options?: readonly string[];
  autoFocus?: boolean;
}

export function TextField({ label, value, onChange, placeholder, hint, options, autoFocus }: TextFieldProps) {
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
        onChange={(e) => onChange(e.target.value)}
      />
      {options && (
        <datalist id={listId}>
          {options.map((o) => (
            <option key={o} value={o} />
          ))}
        </datalist>
      )}
      {hint && <p className="label">{hint}</p>}
    </fieldset>
  );
}

export interface Option {
  value: string;
  label: string;
}

export function SelectField({ label, value, options, onChange }: { label: string; value: string; options: readonly Option[]; onChange: (v: string) => void }) {
  return (
    <fieldset className="fieldset">
      <legend className="fieldset-legend">{label}</legend>
      <select className="select select-sm w-full" value={value} onChange={(e) => onChange(e.target.value)}>
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    </fieldset>
  );
}

export function ToggleField({ label, checked, onChange }: { label: string; checked: boolean; onChange: (v: boolean) => void }) {
  return (
    <fieldset className="fieldset">
      <legend className="fieldset-legend">{label}</legend>
      <label className="label cursor-pointer gap-2">
        <input type="checkbox" className="toggle toggle-sm toggle-primary" checked={checked} onChange={(e) => onChange(e.target.checked)} />
        <span className="text-sm">{checked ? S.common.yes : S.common.no}</span>
      </label>
    </fieldset>
  );
}

/** Структурированная карточка: подпись — значение. */
export function DetailList({ items }: { items: { label: string; value: ReactNode }[] }) {
  return (
    <dl className="grid grid-cols-[max-content_1fr] gap-x-6 gap-y-2 text-sm">
      {items.map((it) => (
        <div key={it.label} className="contents">
          <dt className="opacity-60">{it.label}</dt>
          <dd className="font-medium">{it.value}</dd>
        </div>
      ))}
    </dl>
  );
}

/** Сообщение об ошибке сервиса: код, текст и перечень нарушенных ограничений, если сервис его прислал. */
export function ErrorAlert({ error, onClose }: { error: ErrorDto; onClose?: () => void }) {
  const title = S.errors.byCode[error.code] ?? S.errors.other;
  const details = error.details ?? [];
  const tone = error.code === 422 || error.code === 400 || error.code === 409 ? 'alert-warning' : 'alert-error';
  return (
    <div role="alert" className={`alert ${tone} alert-soft items-start`}>
      <div className="grow">
        <div className="font-semibold">
          {title}
          {error.code > 0 && <span className="badge badge-sm badge-ghost ml-2">{error.code}</span>}
        </div>
        {error.message !== title && <div className="text-sm">{error.message}</div>}
        {details.length > 0 && (
          <ul className="mt-2 list-disc pl-5 text-sm">
            {details.map((d) => (
              <li key={d}>{d}</li>
            ))}
          </ul>
        )}
      </div>
      {onClose && (
        <button type="button" className="btn btn-ghost btn-xs" onClick={onClose} aria-label={S.common.close}>
          ✕
        </button>
      )}
    </div>
  );
}

export function Toast({ message }: { message: string }) {
  return (
    <div className="toast toast-end z-50">
      <div role="status" className="alert alert-success shadow-lg">
        <span>{message}</span>
      </div>
    </div>
  );
}

export function Modal({ open, title, onClose, children, wide }: { open: boolean; title: string; onClose: () => void; children: ReactNode; wide?: boolean }) {
  return (
    <dialog className={`modal ${open ? 'modal-open' : ''}`}>
      <div className={`modal-box ${wide ? 'max-w-4xl' : ''}`}>
        <h3 className="mb-3 text-lg font-bold">{title}</h3>
        {children}
      </div>
      <form method="dialog" className="modal-backdrop">
        <button type="button" onClick={onClose}>
          {S.common.close}
        </button>
      </form>
    </dialog>
  );
}

export function EmptyState({ text }: { text: string }) {
  return <div className="py-10 text-center text-sm opacity-60">{text}</div>;
}

export function Hint({ children }: { children: ReactNode }) {
  return <p className="text-xs opacity-60">{children}</p>;
}

/** Заголовок сортируемой колонки: клик переключает направление, индекс показывает приоритет ступени. */
export function SortableTh({ title, mark, onClick }: { title: string; mark: { dir: string; priority: number; multi: boolean } | null; onClick: () => void }) {
  return (
    <th className="cursor-pointer select-none whitespace-nowrap hover:bg-base-200" onClick={onClick}>
      {title}
      {mark && (
        <span className="ml-1 text-primary">
          {mark.dir}
          {mark.multi && <sup>{mark.priority}</sup>}
        </span>
      )}
    </th>
  );
}

/** Строка состояния сортировки над таблицей. */
export function SortSummary({ sort, loading, onReset }: { sort: string[]; loading: boolean; onReset: () => void }) {
  return (
    <div className="flex flex-wrap items-center gap-2 px-4 pt-3 text-sm">
      <span className="opacity-60">{S.common.sorting}:</span>
      {sort.length === 0 ? (
        <span className="opacity-60">{S.common.sortDefault}</span>
      ) : (
        <>
          {sort.map((t) => (
            <span key={t} className="badge badge-outline badge-sm">
              {t.startsWith('-') ? `${t.slice(1)} ↓` : `${t} ↑`}
            </span>
          ))}
          <button type="button" className="btn btn-ghost btn-xs" onClick={onReset}>
            {S.common.reset}
          </button>
        </>
      )}
      {loading && <span className="loading loading-spinner loading-xs ml-auto" />}
    </div>
  );
}

/** Общая для страниц логика многоступенчатой сортировки: asc → desc → без сортировки. */
export function toggleSortToken(current: string[], field: string): string[] {
  if (current.includes(field)) return [...current.filter((t) => t !== field), `-${field}`];
  if (current.includes(`-${field}`)) return current.filter((t) => t !== `-${field}`);
  return [...current, field];
}

export function sortMarkOf(sort: string[], field: string): { dir: string; priority: number; multi: boolean } | null {
  const i = sort.findIndex((t) => t === field || t === `-${field}`);
  if (i < 0) return null;
  return { dir: sort[i] === field ? '↑' : '↓', priority: i + 1, multi: sort.length > 1 };
}
