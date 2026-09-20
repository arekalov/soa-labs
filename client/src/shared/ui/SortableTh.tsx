import type { SortMark } from '@/shared/lib';

/** Клик переключает направление, индекс показывает приоритет ступени. */
export function SortableTh({ title, mark, onClick }: { title: string; mark: SortMark | null; onClick: () => void }) {
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
