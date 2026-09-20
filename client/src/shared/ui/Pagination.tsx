import { S } from '@/shared/config';

const SIZES = [10, 20, 50, 100];
const WINDOW = 7;

interface Props {
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onPage: (page: number) => void;
  onSize: (size: number) => void;
}

function windowAround(page: number, last: number): number[] {
  const start = Math.max(0, Math.min(page - Math.floor(WINDOW / 2), last - WINDOW + 1));
  const end = Math.min(last, start + WINDOW - 1);
  const result: number[] = [];
  for (let current = start; current <= end; current++) result.push(current);
  return result;
}

export function Pagination({ page, totalPages, totalElements, size, onPage, onSize }: Props) {
  const last = Math.max(0, totalPages - 1);
  return (
    <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
      <div className="text-sm opacity-70">
        {S.common.total}: {totalElements} · {S.common.page} {page + 1} {S.common.of} {Math.max(1, totalPages)}
      </div>
      <div className="flex items-center gap-3">
        <div className="join">
          <button type="button" className="join-item btn btn-sm" disabled={page === 0} onClick={() => onPage(0)}>
            «
          </button>
          <button type="button" className="join-item btn btn-sm" disabled={page === 0} onClick={() => onPage(page - 1)}>
            ‹
          </button>
          {windowAround(page, last).map((number) => (
            <button
              type="button"
              key={number}
              className={`join-item btn btn-sm ${number === page ? 'btn-active btn-primary' : ''}`}
              onClick={() => onPage(number)}
            >
              {number + 1}
            </button>
          ))}
          <button type="button" className="join-item btn btn-sm" disabled={page >= last} onClick={() => onPage(page + 1)}>
            ›
          </button>
          <button type="button" className="join-item btn btn-sm" disabled={page >= last} onClick={() => onPage(last)}>
            »
          </button>
        </div>
        <select className="select select-sm w-28" value={size} onChange={(event) => onSize(Number(event.target.value))}>
          {SIZES.map((option) => (
            <option key={option} value={option}>
              {S.common.perPage(option)}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}
