import { S } from '@/shared/config';

export function SortSummary({ sort, loading, onReset }: { sort: string[]; loading: boolean; onReset: () => void }) {
  return (
    <div className="flex flex-wrap items-center gap-2 px-4 pt-3 text-sm">
      <span className="opacity-60">{S.common.sorting}:</span>
      {sort.length === 0 ? (
        <span className="opacity-60">{S.common.sortDefault}</span>
      ) : (
        <>
          {sort.map((token) => (
            <span key={token} className="badge badge-outline badge-sm">
              {token.startsWith('-') ? `${token.slice(1)} ↓` : `${token} ↑`}
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
