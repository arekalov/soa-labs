import type { ReactNode } from 'react';
import type { PageDto } from '@/shared/api';
import { S } from '@/shared/config';
import { sortMarkOf } from '@/shared/lib';
import { EmptyState } from './EmptyState';
import { Pagination } from './Pagination';
import { SortableTh } from './SortableTh';
import { SortSummary } from './SortSummary';

export interface Column<T> {
  /** Совпадает с именем поля в параметре `sort`, если колонка сортируемая. */
  key: string;
  title: string;
  sortable?: boolean;
  render: (item: T) => ReactNode;
}

interface Props<T> {
  columns: Column<T>[];
  data: PageDto<T> | null;
  loading: boolean;
  sort: string[];
  size: number;
  rowKey: (item: T) => number;
  onToggleSort: (key: string) => void;
  onResetSort: () => void;
  onRowClick: (item: T) => void;
  rowActions: (item: T) => ReactNode;
  onPage: (page: number) => void;
  onSize: (size: number) => void;
}

/** Таблица со страницами и сортировкой. Одинакова для десантников и кораблей. */
export function EntityTable<T>({
  columns,
  data,
  loading,
  sort,
  size,
  rowKey,
  onToggleSort,
  onResetSort,
  onRowClick,
  rowActions,
  onPage,
  onSize,
}: Props<T>) {
  return (
    <div className="card bg-base-100 shadow-sm">
      <div className="card-body gap-2 p-0">
        <SortSummary sort={sort} loading={loading} onReset={onResetSort} />

        <div className="overflow-x-auto">
          <table className="table table-zebra table-sm">
            <thead>
              <tr>
                {columns.map((column) =>
                  column.sortable === false ? (
                    <th key={column.key}>{column.title}</th>
                  ) : (
                    <SortableTh
                      key={column.key}
                      title={column.title}
                      mark={sortMarkOf(sort, column.key)}
                      onClick={() => onToggleSort(column.key)}
                    />
                  ),
                )}
                <th />
              </tr>
            </thead>
            <tbody>
              {data?.items.map((item) => (
                <tr key={rowKey(item)} className="hover cursor-pointer" onClick={() => onRowClick(item)}>
                  {columns.map((column) => (
                    <td key={column.key} className="max-w-56 truncate">
                      {column.render(item)}
                    </td>
                  ))}
                  <td className="whitespace-nowrap text-right" onClick={(event) => event.stopPropagation()}>
                    {rowActions(item)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {data && data.items.length === 0 && <EmptyState text={S.common.nothingFound} />}
          {!data && <EmptyState text={S.common.loading} />}
        </div>

        {data && (
          <div className="px-4 pb-4">
            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              size={size}
              onPage={onPage}
              onSize={onSize}
            />
          </div>
        )}
      </div>
    </div>
  );
}
