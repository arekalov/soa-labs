import { useState } from 'react';
import type { SoaClient } from '../api/client';
import type { ErrorDto, StarshipDto, UnloadResultDto } from '../api/types';
import { ErrorAlert, Hint, PageHeader, Panel, TextField } from '../components/ui';

/**
 * Обе операции второго сервиса.
 *
 * Высадка примечательна тем, что второй сервис при её выполнении сам обращается
 * к первому по HTTPS. Поэтому здесь возможен ответ 503 — он означает, что недоступен
 * именно первый сервис, а не тот, к которому мы обратились.
 */
export function StarshipsPage({ client }: { client: SoaClient }) {
  const [shipId, setShipId] = useState('1');
  const [shipName, setShipName] = useState("Macragge's Honour");
  const [created, setCreated] = useState<StarshipDto | null>(null);
  const [createError, setCreateError] = useState<ErrorDto | null>(null);

  const [unloadShip, setUnloadShip] = useState('1');
  const [unloadMarine, setUnloadMarine] = useState('1');
  const [unloaded, setUnloaded] = useState<UnloadResultDto | null>(null);
  const [unloadError, setUnloadError] = useState<ErrorDto | null>(null);

  const [busy, setBusy] = useState<'create' | 'unload' | null>(null);

  const create = async () => {
    setBusy('create');
    const r = await client.createStarship(shipId, shipName);
    setBusy(null);
    if (r.ok) {
      setCreated(r.value);
      setCreateError(null);
    } else {
      setCreated(null);
      setCreateError(r.error);
    }
  };

  const unload = async () => {
    setBusy('unload');
    const r = await client.unload(unloadShip, unloadMarine);
    setBusy(null);
    if (r.ok) {
      setUnloaded(r.value);
      setUnloadError(null);
    } else {
      setUnloaded(null);
      setUnloadError(r.error);
    }
  };

  return (
    <>
      <PageHeader title="Десантные корабли" subtitle="Второй сервис: располагается на /starship и вызывает REST API первого" />

      <div className="grid gap-4 lg:grid-cols-2">
        <Panel title="Создать корабль">
          <Hint>
            <code>POST /starship/create/{'{id}'}/{'{name}'}</code> — создать десантный корабль и сохранить его в БД. Идентификатор задаёт клиент.
          </Hint>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              void create();
            }}
          >
            <div className="grid grid-cols-3 gap-x-4">
              <TextField label="ID (> 0)" value={shipId} onChange={setShipId} />
              <div className="col-span-2">
                <TextField label="Название" value={shipName} onChange={setShipName} />
              </div>
            </div>
            <div className="card-actions mt-1">
              <button type="submit" className="btn btn-sm btn-primary" disabled={busy !== null}>
                {busy === 'create' && <span className="loading loading-spinner loading-xs" />}
                Создать
              </button>
            </div>
          </form>
          {createError && <ErrorAlert error={createError} onClose={() => setCreateError(null)} />}
          {created && (
            <div role="status" className="alert alert-success alert-soft">
              <div>
                <div className="font-semibold">Корабль создан и сохранён в базе данных</div>
                <div className="text-sm">
                  №{created.id} — «{created.name}». {created.marines.length === 0 ? 'На борту сейчас никого нет.' : `На борту десантники: ${created.marines.join(', ')}.`}
                </div>
              </div>
            </div>
          )}
          <Hint>Повторное создание корабля с тем же идентификатором даёт 409 — так требует спецификация.</Hint>
        </Panel>

        <Panel title="Высадить десантника">
          <Hint>
            <code>POST /starship/{'{starship-id}'}/unload/{'{space-marine-id}'}</code> — сервис проверяет существование десантника запросом к первому сервису.
          </Hint>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              void unload();
            }}
          >
            <div className="grid grid-cols-2 gap-x-4">
              <TextField label="ID корабля" value={unloadShip} onChange={setUnloadShip} />
              <TextField label="ID десантника" value={unloadMarine} onChange={setUnloadMarine} />
            </div>
            <div className="card-actions mt-1">
              <button type="submit" className="btn btn-sm btn-primary" disabled={busy !== null}>
                {busy === 'unload' && <span className="loading loading-spinner loading-xs" />}
                Высадить
              </button>
            </div>
          </form>
          {unloadError && <ErrorAlert error={unloadError} onClose={() => setUnloadError(null)} />}
          {unloaded && (
            <div role="status" className="alert alert-success alert-soft">
              <div>
                <div className="font-semibold">Высадка выполнена</div>
                <div className="text-sm">{unloaded.message}</div>
              </div>
            </div>
          )}
          <Hint>
            404 приходит в трёх случаях — корабля нет, десантника нет в первом сервисе, десантник не на этом корабле; сообщение уточняет, какой именно. 503
            означает, что первый сервис недоступен.
          </Hint>
        </Panel>
      </div>
    </>
  );
}
