import { useState } from 'react';
import type { SoaClient } from '../api/client';
import type { ErrorDto, IdGroupDto, SpaceMarineDto } from '../api/types';
import { EmptyState, ErrorAlert, Hint, PageHeader, Panel, TextField } from '../components/ui';

/**
 * Три дополнительные операции первого сервиса.
 *
 * Каждая показана в естественном для неё виде: карточка-показатель для одного объекта,
 * таблица для группировки, число для подсчёта.
 */
export function ExtrasPage({ client }: { client: SoaClient }) {
  const [error, setError] = useState<ErrorDto | null>(null);
  const [minHealth, setMinHealth] = useState<SpaceMarineDto | null>(null);
  const [groups, setGroups] = useState<IdGroupDto[] | null>(null);
  const [chapterName, setChapterName] = useState('Ultramarines');
  const [parentLegion, setParentLegion] = useState('');
  const [count, setCount] = useState<{ text: string; value: number } | null>(null);
  const [busy, setBusy] = useState<'min' | 'groups' | 'count' | null>(null);

  const loadMin = async () => {
    setBusy('min');
    const r = await client.minHealth();
    setBusy(null);
    if (r.ok) {
      setError(null);
      setMinHealth(r.value);
    } else {
      setMinHealth(null);
      setError(r.error);
    }
  };

  const loadGroups = async () => {
    setBusy('groups');
    const r = await client.groupsById();
    setBusy(null);
    if (r.ok) {
      setError(null);
      setGroups(r.value);
    } else {
      setGroups(null);
      setError(r.error);
    }
  };

  const loadCount = async () => {
    setBusy('count');
    const r = await client.countByChapter(chapterName, parentLegion);
    setBusy(null);
    if (r.ok) {
      setError(null);
      const legion = parentLegion.trim() === '' ? 'любого легиона' : `легиона «${parentLegion}»`;
      setCount({ value: r.value.count, text: `В ордене «${chapterName}» (${legion}) числится десантников: ${r.value.count}.` });
    } else {
      setCount(null);
      setError(r.error);
    }
  };

  const spinner = (key: typeof busy) => busy === key && <span className="loading loading-spinner loading-xs" />;

  return (
    <>
      <PageHeader title="Сводные операции" subtitle="Дополнительные операции первого сервиса, размещённые на отдельных URL" />

      {error && (
        <div className="mb-4">
          <ErrorAlert error={error} onClose={() => setError(null)} />
        </div>
      )}

      <div className="grid gap-4 lg:grid-cols-3">
        <Panel title="Минимальное здоровье">
          <Hint>Вернуть любой объект, значение поля health которого минимально.</Hint>
          {minHealth && (
            <div className="stats bg-base-200">
              <div className="stat">
                <div className="stat-title">Здоровье</div>
                <div className="stat-value text-primary">{minHealth.health}</div>
                <div className="stat-desc">
                  №{minHealth.id} · {minHealth.name} · {minHealth.category}
                  {minHealth.chapter?.name ? ` · ${minHealth.chapter.name}` : ''}
                </div>
              </div>
            </div>
          )}
          <div className="card-actions">
            <button type="button" className="btn btn-sm btn-primary" onClick={() => void loadMin()} disabled={busy !== null}>
              {spinner('min')}Запросить
            </button>
          </div>
          <Hint>На пустой коллекции сервис отвечает 404 — так предусмотрено спецификацией.</Hint>
        </Panel>

        <Panel title="Группировка по id">
          <Hint>Сгруппировать объекты по значению поля id и вернуть количество элементов в каждой группе.</Hint>
          {groups &&
            (groups.length === 0 ? (
              <EmptyState text="Коллекция пуста — групп нет" />
            ) : (
              <div className="max-h-72 overflow-auto rounded-box border border-base-300">
                <table className="table table-sm table-pin-rows">
                  <thead>
                    <tr>
                      <th>Значение id</th>
                      <th>Элементов</th>
                    </tr>
                  </thead>
                  <tbody>
                    {groups.map((g) => (
                      <tr key={g.id}>
                        <td>{g.id}</td>
                        <td>{g.count}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ))}
          <div className="card-actions">
            <button type="button" className="btn btn-sm btn-primary" onClick={() => void loadGroups()} disabled={busy !== null}>
              {spinner('groups')}Сгруппировать
            </button>
          </div>
          <Hint>Идентификатор уникален, поэтому в каждой группе ожидаемо ровно один элемент.</Hint>
        </Panel>

        <Panel title="Число десантников ордена">
          <Hint>Вернуть количество объектов, значение поля chapter которых равно заданному.</Hint>
          <TextField label="Орден (обязательно)" value={chapterName} onChange={setChapterName} />
          <TextField label="Легион" value={parentLegion} onChange={setParentLegion} hint="пусто — любой легион" />
          {count && (
            <div className="stats bg-base-200">
              <div className="stat">
                <div className="stat-title">Десантников</div>
                <div className="stat-value text-primary">{count.value}</div>
                <div className="stat-desc whitespace-normal">{count.text}</div>
              </div>
            </div>
          )}
          <div className="card-actions">
            <button type="button" className="btn btn-sm btn-primary" onClick={() => void loadCount()} disabled={busy !== null}>
              {spinner('count')}Посчитать
            </button>
          </div>
        </Panel>
      </div>
    </>
  );
}
