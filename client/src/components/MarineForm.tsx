import { useState } from 'react';
import { CATEGORIES, type SpaceMarineDto, type SpaceMarineInputDto } from '../api/types';
import type { ChapterOptions } from '../hooks/useChapterOptions';
import { S } from '../strings';
import { SelectField, TextField, ToggleField } from './ui';

interface Props {
  initial?: SpaceMarineDto | null;
  options: ChapterOptions;
  submitLabel: string;
  busy?: boolean;
  onSubmit: (input: SpaceMarineInputDto) => void;
  onCancel: () => void;
}

const numberOrNull = (raw: string): number | null => {
  const n = Number(raw);
  return raw.trim() === '' || Number.isNaN(n) ? null : n;
};

const textOrNull = (raw: string): string | null => (raw.trim() === '' ? null : raw);

/** Полный набор полей десантника в виде тела `SpaceMarineInput`. */
export function marineToInput(m: SpaceMarineDto): SpaceMarineInputDto {
  return {
    name: m.name,
    coordinates: { x: m.coordinates.x, y: m.coordinates.y },
    health: m.health,
    loyal: m.loyal,
    achievements: m.achievements,
    category: m.category,
    chapter: m.chapter ? { name: m.chapter.name, parentLegion: m.chapter.parentLegion } : null,
  };
}

/**
 * Одна форма на создание и изменение.
 *
 * Поля не проверяются на клиенте намеренно: ограничения целостности — зона
 * ответственности сервиса, а его ответ 422 с перечнем нарушений показывается как есть.
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

  const f = S.marine.fields;

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit(build());
      }}
    >
      <div className="grid grid-cols-1 gap-x-4 md:grid-cols-3">
        <TextField label={f.name} value={name} onChange={setName} autoFocus />
        <SelectField label={f.category} value={category} options={CATEGORIES.map((c) => ({ value: c, label: c }))} onChange={setCategory} />
        <TextField label={f.health} value={health} onChange={setHealth} />
        <TextField label={f.x} value={x} onChange={setX} />
        <TextField label={f.y} value={y} onChange={setY} />
        <ToggleField label={f.loyal} checked={loyal} onChange={setLoyal} />
        <TextField label={f.achievements} value={achievements} onChange={setAchievements} />
        <TextField label={f.chapter} value={chapterName} onChange={setChapterName} options={options.chapters} />
        <TextField label={f.legion} value={chapterLegion} onChange={setChapterLegion} options={options.legions} />
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

const INPUT_KEYS = ['name', 'coordinates', 'health', 'loyal', 'achievements', 'category', 'chapter'] as const;

/** Только изменившиеся поля — тело для PATCH. Пустой объект означает, что менять нечего. */
export function diffInput(before: SpaceMarineInputDto, after: SpaceMarineInputDto): SpaceMarineInputDto {
  const patch: Record<string, unknown> = {};
  for (const key of INPUT_KEYS) {
    if (JSON.stringify(before[key] ?? null) !== JSON.stringify(after[key] ?? null)) patch[key] = after[key] ?? null;
  }
  return patch as SpaceMarineInputDto;
}
