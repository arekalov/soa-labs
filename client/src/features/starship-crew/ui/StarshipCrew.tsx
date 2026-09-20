import { useState } from 'react';
import type { SpaceMarineDto } from '@/entities/space-marine';
import { boardMarine, getStarship, unloadMarine } from '@/entities/starship';
import type { StarshipDto } from '@/entities/starship';
import type { ErrorDto } from '@/shared/api';
import { S } from '@/shared/config';
import { SelectField } from '@/shared/ui';

interface Props {
  starship: StarshipDto;
  /** Все десантники первого сервиса: для подписей экипажа и выбора при посадке. */
  marines: SpaceMarineDto[];
  /** Кто уже занят: десантник может находиться только на одном корабле. */
  boarded: Set<number>;
  onChanged: (starship: StarshipDto) => void;
  onMessage: (message: string) => void;
  onError: (error: ErrorDto) => void;
}

/** Состав экипажа: посадка и высадка десантников. */
export function StarshipCrew({ starship, marines, boarded, onChanged, onMessage, onError }: Props) {
  const [candidate, setCandidate] = useState('');
  const [busy, setBusy] = useState(false);

  const label = (id: number) => {
    const marine = marines.find((item) => item.id === id);
    return marine ? `№${id} · ${marine.name}` : `№${id}`;
  };

  const board = async () => {
    const marineId = Number(candidate);
    setBusy(true);
    const result = await boardMarine(starship.id, marineId);
    setBusy(false);
    if (!result.ok) return onError(result.error);

    setCandidate('');
    onMessage(S.ship.boarded(marineId, starship.id));
    onChanged(result.value);
  };

  const unload = async (marineId: number) => {
    setBusy(true);
    const result = await unloadMarine(starship.id, marineId);
    if (!result.ok) {
      setBusy(false);
      return onError(result.error);
    }
    onMessage(result.value.message);

    // Высадка возвращает описание результата, а не корабль, поэтому состав перечитываем.
    const fresh = await getStarship(starship.id);
    setBusy(false);
    if (fresh.ok) onChanged(fresh.value);
  };

  const free = marines.filter((marine) => !boarded.has(marine.id));

  return (
    <>
      <div className="divider my-3 text-sm">{S.ship.crew}</div>
      {starship.marines.length === 0 ? (
        <p className="text-sm opacity-60">{S.ship.noCrew}</p>
      ) : (
        <ul className="menu menu-sm w-full rounded-box bg-base-200 p-1">
          {starship.marines.map((id) => (
            <li key={id}>
              <div className="flex justify-between">
                <span>{label(id)}</span>
                <button type="button" className="btn btn-ghost btn-xs" disabled={busy} onClick={() => void unload(id)}>
                  {S.ship.unload}
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <form
        className="mt-3 flex items-end gap-2"
        onSubmit={(event) => {
          event.preventDefault();
          void board();
        }}
      >
        <div className="grow">
          <SelectField
            label={S.ship.marineToBoard}
            value={candidate}
            options={[
              { value: '', label: S.common.empty },
              ...free.map((marine) => ({ value: String(marine.id), label: label(marine.id) })),
            ]}
            onChange={setCandidate}
          />
        </div>
        <button type="submit" className="btn btn-sm btn-primary mb-1" disabled={busy || candidate === ''}>
          {S.ship.board}
        </button>
      </form>
    </>
  );
}
