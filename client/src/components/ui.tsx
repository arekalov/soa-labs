import type { ReactNode } from 'react';
import type { ErrorDto } from '../api/types';

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
}

export function TextField({ label, value, onChange, placeholder, hint }: TextFieldProps) {
  return (
    <fieldset className="fieldset">
      <legend className="fieldset-legend">{label}</legend>
      <input type="text" className="input input-sm w-full" value={value} placeholder={placeholder} onChange={(e) => onChange(e.target.value)} />
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
        <span className="text-sm">{checked ? 'да' : 'нет'}</span>
      </label>
    </fieldset>
  );
}

export function TextArea({ label, value, onChange, rows = 4, hint }: { label: string; value: string; onChange: (v: string) => void; rows?: number; hint?: string }) {
  return (
    <fieldset className="fieldset">
      <legend className="fieldset-legend">{label}</legend>
      <textarea className="textarea textarea-sm w-full font-mono" rows={rows} value={value} onChange={(e) => onChange(e.target.value)} />
      {hint && <p className="label">{hint}</p>}
    </fieldset>
  );
}

const TITLES: Record<number, string> = {
  0: 'Нет связи с сервисом',
  400: 'Некорректный запрос',
  404: 'Не найдено',
  409: 'Конфликт',
  422: 'Данные нарушают ограничения',
  503: 'Сервис недоступен',
};

/**
 * Сообщение об ошибке сервиса.
 *
 * Показывает код, текст и перечень нарушенных ограничений из `details` — именно этого
 * требует задание: пользователь должен понять, что данные невалидны и какие именно.
 */
export function ErrorAlert({ error, onClose }: { error: ErrorDto; onClose?: () => void }) {
  const title = TITLES[error.code] ?? 'Ошибка';
  const details = error.details ?? [];
  const tone = error.code === 422 || error.code === 400 || error.code === 409 ? 'alert-warning' : 'alert-error';
  return (
    <div role="alert" className={`alert ${tone} alert-soft items-start`}>
      <div className="grow">
        <div className="font-semibold">
          {title}
          {error.code > 0 && <span className="badge badge-sm badge-ghost ml-2">{error.code}</span>}
        </div>
        <div className="text-sm">{error.message}</div>
        {details.length > 0 && (
          <ul className="mt-2 list-disc pl-5 text-sm">
            {details.map((d) => (
              <li key={d}>{d}</li>
            ))}
          </ul>
        )}
      </div>
      {onClose && (
        <button type="button" className="btn btn-ghost btn-xs" onClick={onClose} aria-label="Закрыть">
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
          закрыть
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
