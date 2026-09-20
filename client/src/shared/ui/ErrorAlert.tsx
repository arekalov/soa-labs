import type { ErrorDto } from '@/shared/api';
import { S } from '@/shared/config';

/** Код, текст и перечень нарушенных ограничений, если сервис его прислал. */
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
            {details.map((detail) => (
              <li key={detail}>{detail}</li>
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
