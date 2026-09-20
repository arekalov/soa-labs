import { useState } from 'react';
import { CATEGORIES } from '@/entities/space-marine';
import type { ChapterOptions } from '@/entities/space-marine';
import { S } from '@/shared/config';
import { FilterPanel, SelectField, TextField } from '@/shared/ui';
import { countActive, EMPTY_MARINE_FILTERS, type MarineFilters } from '../model/filters';

const F = S.marine.fields;

/** Условия объединяются по «И», сравнение — на точное совпадение. */
export function MarineFiltersForm({ options, onChange }: { options: ChapterOptions; onChange: (filters: MarineFilters) => void }) {
  const [draft, setDraft] = useState<MarineFilters>(EMPTY_MARINE_FILTERS);
  const [activeCount, setActiveCount] = useState(0);

  const set = (field: keyof MarineFilters) => (value: string) => setDraft({ ...draft, [field]: value });

  return (
    <FilterPanel
      activeCount={activeCount}
      onApply={() => {
        setActiveCount(countActive(draft));
        onChange(draft);
      }}
      onReset={() => {
        setDraft(EMPTY_MARINE_FILTERS);
        setActiveCount(0);
        onChange(EMPTY_MARINE_FILTERS);
      }}
    >
      <TextField label={F.id} value={draft.id} onChange={set('id')} />
      <TextField label={F.name} value={draft.name} onChange={set('name')} />
      <SelectField
        label={F.category}
        value={draft.category}
        options={[{ value: '', label: S.common.any }, ...CATEGORIES.map((item) => ({ value: item, label: item }))]}
        onChange={set('category')}
      />
      <SelectField
        label={F.loyal}
        value={draft.loyal}
        options={[
          { value: '', label: S.common.anyone },
          { value: 'true', label: S.common.yes },
          { value: 'false', label: S.common.no },
        ]}
        onChange={set('loyal')}
      />
      <TextField label={F.health} value={draft.health} onChange={set('health')} />
      <TextField label={F.x} value={draft.coordinatesX} onChange={set('coordinatesX')} />
      <TextField label={F.y} value={draft.coordinatesY} onChange={set('coordinatesY')} />
      <TextField
        label={F.created}
        value={draft.creationDate}
        onChange={set('creationDate')}
        placeholder="2026-09-19T10:26:46.736Z"
      />
      <TextField label={F.chapter} value={draft.chapterName} onChange={set('chapterName')} options={options.chapters} />
      <TextField
        label={F.legion}
        value={draft.chapterParentLegion}
        onChange={set('chapterParentLegion')}
        options={options.legions}
      />
      <TextField label={F.achievements} value={draft.achievements} onChange={set('achievements')} />
    </FilterPanel>
  );
}
