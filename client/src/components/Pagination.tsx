interface Props {
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onPage: (page: number) => void;
  onSize: (size: number) => void;
}

const SIZES = [10, 20, 50, 100];

/** Окно из семи номеров вокруг текущей страницы, чтобы полоса не разрасталась. */
function windowAround(page: number, last: number, width = 7): number[] {
  const start = Math.max(0, Math.min(page - Math.floor(width / 2), last - width + 1));
  const end = Math.min(last, start + width - 1);
  const result: number[] = [];
  for (let p = start; p <= end; p++) result.push(p);
  return result;
}

export function Pagination({ page, totalPages, totalElements, size, onPage, onSize }: Props) {
  const last = Math.max(0, totalPages - 1);
  return (
    <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
      <div className="text-sm opacity-70">
        Всего элементов: {totalElements} · страница {page + 1} из {Math.max(1, totalPages)}
      </div>
      <div className="flex items-center gap-3">
        <div className="join">
          <button type="button" className="join-item btn btn-sm" disabled={page === 0} onClick={() => onPage(0)}>
            «
          </button>
          <button type="button" className="join-item btn btn-sm" disabled={page === 0} onClick={() => onPage(page - 1)}>
            ‹
          </button>
          {windowAround(page, last).map((p) => (
            <button type="button" key={p} className={`join-item btn btn-sm ${p === page ? 'btn-active btn-primary' : ''}`} onClick={() => onPage(p)}>
              {p + 1}
            </button>
          ))}
          <button type="button" className="join-item btn btn-sm" disabled={page >= last} onClick={() => onPage(page + 1)}>
            ›
          </button>
          <button type="button" className="join-item btn btn-sm" disabled={page >= last} onClick={() => onPage(last)}>
            »
          </button>
        </div>
        <select className="select select-sm w-28" value={size} onChange={(e) => onSize(Number(e.target.value))}>
          {SIZES.map((s) => (
            <option key={s} value={s}>
              по {s}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
