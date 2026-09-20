import type { StarshipDto, StarshipPageDto } from '@/entities/starship';
import { S } from '@/shared/config';
import { EntityTable } from '@/shared/ui';
import type { Column } from '@/shared/ui';

const F = S.ship.fields;

const COLUMNS: Column<StarshipDto>[] = [
  { key: 'id', title: F.id, render: (starship) => starship.id },
  { key: 'name', title: F.name, render: (starship) => starship.name },
  { key: 'crew', title: F.crew, sortable: false, render: (starship) => S.ship.crewSize(starship.marines.length) },
];

interface Props {
  data: StarshipPageDto | null;
  loading: boolean;
  sort: string[];
  size: number;
  onToggleSort: (field: string) => void;
  onResetSort: () => void;
  onOpen: (starship: StarshipDto) => void;
  onEdit: (starship: StarshipDto) => void;
  onDelete: (starship: StarshipDto) => void;
  onPage: (page: number) => void;
  onSize: (size: number) => void;
}

export function StarshipsTable({ onOpen, onEdit, onDelete, ...rest }: Props) {
  return (
    <EntityTable
      {...rest}
      columns={COLUMNS}
      rowKey={(starship) => starship.id}
      onRowClick={onOpen}
      rowActions={(starship) => (
        <>
          <button type="button" className="btn btn-ghost btn-xs" onClick={() => onEdit(starship)}>
            {S.common.edit}
          </button>
          <button type="button" className="btn btn-ghost btn-xs text-error" onClick={() => onDelete(starship)}>
            {S.common.delete}
          </button>
        </>
      )}
    />
  );
}
