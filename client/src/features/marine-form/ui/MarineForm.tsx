import { useState } from 'react';
import { CATEGORIES } from '@/entities/space-marine';
import type { ChapterOptions, SpaceMarineDto, SpaceMarineInputDto } from '@/entities/space-marine';
import { S } from '@/shared/config';
import { SelectField, TextField, ToggleField } from '@/shared/ui';

interface Props {
  initial?: SpaceMarineDto | null;
  options: ChapterOptions;
  submitLabel: string;
  busy?: boolean;
  onSubmit: (input: SpaceMarineInputDto) => void;
  onCancel: () => void;
}

const numberOrNull = (raw: string): number | null => {
  const value = Number(raw);
  return raw.trim() === '' || Number.isNaN(value) ? null : value;
};

const textOrNull = (raw: string): string | null => (raw.trim() === '' ? null : raw);

/**
 * Одна форма на создание и изменение.
 *
 * Поля не проверяются на клиенте намеренно: ограничения целостности — зона ответственности
 * сервиса, а его ответ 422 с перечнем нарушений показывается как есть.
 */
export function MarineForm({ initial, options, submitLabel, busy, onSubmit, onCancel }: Props) {
  const [name, setName] = useState(initial?.name ?? '');
  const [category, setCategory] = useState<string>(initial?.category ?? 'TACTICAL');
  const [health, setHealth] = useState(initial ? String(initial.health) : '');
  const [loyal, setLoyal] = useState(initial?.loyal ?? true);
  const [x, setX] = useState(initial?.coordinates.x != null ? String(initial.coordinates.x) : '');
  const [y, setY] = useState(initial?.coordinates.y != null ? String(initial.coordinates.y) : '');
  const [achievements, setAchievements] = useState(initial?.achievements ?? '');
  const [chapterName, setChapterName] = useState(initial?.chapter?.name ?? '');
  const [chapterLegion, setChapterLegion] = useState(initial?.chapter?.parentLegion ?? '');

  const build = (): SpaceMarineInputDto => ({
    name: textOrNull(name),
    coordinates: { x: numberOrNull(x), y: numberOrNull(y) },
    health: numberOrNull(health),
    loyal,
    achievements: textOrNull(achievements),
    category: category === '' ? null : category,
    chapter:
      chapterName.trim() === '' && chapterLegion.trim() === ''
        ? null
        : { name: textOrNull(chapterName), parentLegion: textOrNull(chapterLegion) },
  });

  const F = S.marine.fields;

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit(build());
      }}
    >
      <div className="grid grid-cols-1 gap-x-4 md:grid-cols-3">
        <TextField label={F.name} value={name} onChange={setName} autoFocus />
        <SelectField
          label={F.category}
          value={category}
          options={CATEGORIES.map((item) => ({ value: item, label: item }))}
          onChange={setCategory}
        />
        <TextField label={F.health} value={health} onChange={setHealth} />
        <TextField label={F.x} value={x} onChange={setX} />
        <TextField label={F.y} value={y} onChange={setY} />
        <ToggleField label={F.loyal} checked={loyal} onChange={setLoyal} />
        <TextField label={F.achievements} value={achievements} onChange={setAchievements} />
        <TextField label={F.chapter} value={chapterName} onChange={setChapterName} options={options.chapters} />
        <TextField label={F.legion} value={chapterLegion} onChange={setChapterLegion} options={options.legions} />
      </div>
      <div className="modal-action">
        <button type="button" className="btn" onClick={onCancel} disabled={busy}>
          {S.common.cancel}
        </button>
        <button type="submit" className="btn btn-primary" disabled={busy}>
          {busy && <span className="loading loading-spinner loading-xs" />}
          {submitLabel}
        </button>
      </div>
    </form>
  );
}
