import { useState } from 'react';
import { CATEGORIES, type SpaceMarineDto, type SpaceMarineInputDto } from '../api/types';
import { Hint, SelectField, TextField, ToggleField } from './ui';

interface Props {
  initial?: SpaceMarineDto | null;
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

/**
 * Одна форма на создание и полную замену: обе операции принимают схему `SpaceMarineInput`.
 *
 * Поля не проверяются на клиенте намеренно. Ограничения целостности — зона ответственности
 * сервиса, и задание требует показать пользователю именно его ответ 422 с перечнем нарушений.
 */
export function MarineForm({ initial, submitLabel, busy, onSubmit, onCancel }: Props) {
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

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit(build());
      }}
    >
      <div className="grid grid-cols-1 gap-x-4 md:grid-cols-3">
        <TextField label="Имя" value={name} onChange={setName} />
        <SelectField label="Категория" value={category} options={CATEGORIES.map((c) => ({ value: c, label: c }))} onChange={setCategory} />
        <TextField label="Здоровье (> 0)" value={health} onChange={setHealth} />
        <TextField label="Координата X" value={x} onChange={setX} />
        <TextField label="Координата Y (≤ 12)" value={y} onChange={setY} />
        <ToggleField label="Верен Империуму" checked={loyal} onChange={setLoyal} />
        <TextField label="Достижения" value={achievements} onChange={setAchievements} hint="необязательно" />
        <TextField label="Орден" value={chapterName} onChange={setChapterName} hint="необязательно; если указан — имя обязательно" />
        <TextField label="Легион ордена" value={chapterLegion} onChange={setChapterLegion} />
      </div>
      <Hint>Незаполненные поля уходят как отсутствующие — при нарушении ограничений сервис ответит 422 и перечислит, что именно не так.</Hint>
      <div className="modal-action">
        <button type="button" className="btn" onClick={onCancel} disabled={busy}>
          Отмена
        </button>
        <button type="submit" className="btn btn-primary" disabled={busy}>
          {busy && <span className="loading loading-spinner loading-xs" />}
          {submitLabel}
        </button>
      </div>
    </form>
  );
}
