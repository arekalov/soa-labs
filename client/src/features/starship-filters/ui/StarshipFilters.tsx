import { useState } from 'react';
import { S } from '@/shared/config';
import { FilterPanel, TextField } from '@/shared/ui';
import { EMPTY_STARSHIP_FILTERS, type StarshipFilters } from '../model/filters';

const F = S.ship.fields;

export function StarshipFiltersForm({ onChange }: { onChange: (filters: StarshipFilters) => void }) {
  const [draft, setDraft] = useState<StarshipFilters>(EMPTY_STARSHIP_FILTERS);
  const [activeCount, setActiveCount] = useState(0);

  return (
    <FilterPanel
      activeCount={activeCount}
      onApply={() => {
        setActiveCount(Object.values(draft).filter((value) => value.trim() !== '').length);
        onChange(draft);
      }}
      onReset={() => {
        setDraft(EMPTY_STARSHIP_FILTERS);
        setActiveCount(0);
        onChange(EMPTY_STARSHIP_FILTERS);
      }}
    >
      <TextField label={F.id} value={draft.id} onChange={(value) => setDraft({ ...draft, id: value })} />
      <TextField label={F.name} value={draft.name} onChange={(value) => setDraft({ ...draft, name: value })} />
    </FilterPanel>
  );
}
