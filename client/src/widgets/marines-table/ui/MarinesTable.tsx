import type { SortableField, SpaceMarineDto, SpaceMarinePageDto } from '@/entities/space-marine';
import { S } from '@/shared/config';
import { formatDate } from '@/shared/lib';
import { EntityTable } from '@/shared/ui';
import type { Column } from '@/shared/ui';

const F = S.marine.fields;

/** Колонки соответствуют полям спецификации: по каждой можно и фильтровать, и сортировать. */
const COLUMNS: Column<SpaceMarineDto>[] = [
  { key: 'id', title: F.id, render: (marine) => marine.id },
  { key: 'name', title: F.name, render: (marine) => marine.name },
  { key: 'category', title: F.category, render: (marine) => marine.category },
  { key: 'health', title: F.health, render: (marine) => marine.health },
  { key: 'loyal', title: F.loyal, render: (marine) => (marine.loyal ? S.common.yes : S.common.no) },
  { key: 'coordinatesX', title: 'X', render: (marine) => marine.coordinates.x ?? S.common.empty },
  { key: 'coordinatesY', title: 'Y', render: (marine) => marine.coordinates.y ?? S.common.empty },
  { key: 'chapterName', title: F.chapter, render: (marine) => marine.chapter?.name ?? S.common.empty },
  { key: 'chapterParentLegion', title: F.legion, render: (marine) => marine.chapter?.parentLegion ?? S.common.empty },
  { key: 'achievements', title: F.achievements, render: (marine) => marine.achievements ?? S.common.empty },
  { key: 'creationDate', title: F.created, render: (marine) => formatDate(marine.creationDate) },
];

interface Props {
  data: SpaceMarinePageDto | null;
  loading: boolean;
  sort: string[];
  size: number;
  onToggleSort: (field: SortableField) => void;
  onResetSort: () => void;
  onOpen: (marine: SpaceMarineDto) => void;
  onEdit: (marine: SpaceMarineDto) => void;
  onDelete: (marine: SpaceMarineDto) => void;
  onPage: (page: number) => void;
  onSize: (size: number) => void;
}

export function MarinesTable({ onToggleSort, onOpen, onEdit, onDelete, ...rest }: Props) {
  return (
    <EntityTable
      {...rest}
      columns={COLUMNS}
      rowKey={(marine) => marine.id}
      onToggleSort={(key) => onToggleSort(key as SortableField)}
      onRowClick={onOpen}
      rowActions={(marine) => (
        <>
          <button type="button" className="btn btn-ghost btn-xs" onClick={() => onEdit(marine)}>
            {S.common.edit}
          </button>
          <button type="button" className="btn btn-ghost btn-xs text-error" onClick={() => onDelete(marine)}>
            {S.common.delete}
          </button>
        </>
      )}
    />
  );
}
