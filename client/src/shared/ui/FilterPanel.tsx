import type { ReactNode } from 'react';
import { S } from '@/shared/config';

/** Раскрывающаяся панель фильтров: общая оболочка для всех разделов. */
export function FilterPanel({
  activeCount,
  children,
  onApply,
  onReset,
}: {
  activeCount: number;
  children: ReactNode;
  onApply: () => void;
  onReset: () => void;
}) {
  return (
    <div className="collapse collapse-arrow mb-4 bg-base-100 shadow-sm">
      <input type="checkbox" defaultChecked />
      <div className="collapse-title flex items-center gap-2 font-semibold">
        {S.common.filters}
        {activeCount > 0 && <span className="badge badge-primary badge-sm">{activeCount}</span>}
      </div>
      <div className="collapse-content">
        <div className="grid grid-cols-2 gap-x-4 md:grid-cols-4">{children}</div>
        <div className="mt-2 flex flex-wrap items-center gap-2">
          <button type="button" className="btn btn-sm btn-primary" onClick={onApply}>
            {S.common.apply}
          </button>
          <button type="button" className="btn btn-sm btn-ghost" onClick={onReset}>
            {S.common.reset}
          </button>
        </div>
      </div>
    </div>
  );
}
