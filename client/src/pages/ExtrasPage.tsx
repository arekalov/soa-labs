import { useState } from 'react';
import type { SoaClient } from '../api/client';
import type { ErrorDto, SpaceMarineDto } from '../api/types';
import { EmptyState, ErrorAlert, PageHeader, Panel, TextField } from '../components/ui';
import { useChapterOptions } from '../hooks/useChapterOptions';
import { S } from '../strings';

const F = S.marine.fields;

/** Три дополнительные операции первого сервиса — те, что заданы вариантом. */
export function ExtrasPage({ client }: { client: SoaClient }) {
  const options = useChapterOptions(client);

  const [chapter, setChapter] = useState('');
  const [legion, setLegion] = useState('');
  const [chapterCount, setChapterCount] = useState<number | null>(null);
  const [chapterError, setChapterError] = useState<ErrorDto | null>(null);

  const [threshold, setThreshold] = useState('');
  const [healthCount, setHealthCount] = useState<number | null>(null);
  const [healthError, setHealthError] = useState<ErrorDto | null>(null);

  const [prefix, setPrefix] = useState('');
  const [found, setFound] = useState<SpaceMarineDto[] | null>(null);
  const [prefixError, setPrefixError] = useState<ErrorDto | null>(null);

  const countByChapter = async () => {
    const r = await client.countByChapter(chapter, legion);
    if (r.ok) {
      setChapterError(null);
      setChapterCount(r.value.count);
    } else {
      setChapterCount(null);
      setChapterError(r.error);
    }
  };

  const countByHealth = async () => {
    const r = await client.countByHealthGreaterThan(threshold);
    if (r.ok) {
      setHealthError(null);
      setHealthCount(r.value.count);
    } else {
      setHealthCount(null);
      setHealthError(r.error);
    }
  };

  const search = async () => {
    const r = await client.findByNamePrefix(prefix);
    if (r.ok) {
      setPrefixError(null);
      setFound(r.value);
    } else {
      setFound(null);
      setPrefixError(r.error);
    }
  };

  return (
    <>
      <PageHeader title={S.extras.title} subtitle={S.extras.subtitle} />

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <Panel title={S.extras.byChapter}>
          <form
            className="grid grid-cols-2 gap-x-4"
            onSubmit={(e) => {
              e.preventDefault();
              void countByChapter();
            }}
          >
            <TextField label={F.chapter} value={chapter} onChange={setChapter} options={options.chapters} />
            <TextField label={F.legion} value={legion} onChange={setLegion} options={options.legions} hint={S.extras.legionHint} />
            <div className="col-span-2 flex items-center gap-4">
              <button type="submit" className="btn btn-sm btn-primary">
                {S.extras.count}
              </button>
              {chapterCount !== null && (
                <div className="stat px-0 py-0">
                  <div className="stat-title text-xs">{S.extras.marinesCount}</div>
                  <div className="stat-value text-2xl">{chapterCount}</div>
                </div>
              )}
            </div>
          </form>
          {chapterError && <ErrorAlert error={chapterError} onClose={() => setChapterError(null)} />}
        </Panel>

        <Panel title={S.extras.healthAbove}>
          <form
            className="grid grid-cols-2 gap-x-4"
            onSubmit={(e) => {
              e.preventDefault();
              void countByHealth();
            }}
          >
            <TextField label={S.extras.threshold} value={threshold} onChange={setThreshold} />
            <div className="col-span-2 flex items-center gap-4">
              <button type="submit" className="btn btn-sm btn-primary">
                {S.extras.count}
              </button>
              {healthCount !== null && (
                <div className="stat px-0 py-0">
                  <div className="stat-title text-xs">{S.extras.marinesCount}</div>
                  <div className="stat-value text-2xl">{healthCount}</div>
                </div>
              )}
            </div>
          </form>
          {healthError && <ErrorAlert error={healthError} onClose={() => setHealthError(null)} />}
        </Panel>

        <Panel title={S.extras.namePrefix} className="xl:col-span-2">
          <form
            className="flex flex-wrap items-end gap-3"
            onSubmit={(e) => {
              e.preventDefault();
              void search();
            }}
          >
            <div className="w-72">
              <TextField label={S.extras.prefix} value={prefix} onChange={setPrefix} />
            </div>
            <button type="submit" className="btn btn-sm btn-primary mb-1">
              {S.extras.find}
            </button>
            {found && <span className="mb-2 text-sm opacity-70">{S.extras.found(found.length)}</span>}
          </form>
          {prefixError && <ErrorAlert error={prefixError} onClose={() => setPrefixError(null)} />}
          {found && found.length === 0 && <EmptyState text={S.common.nothingFound} />}
          {found && found.length > 0 && (
            <div className="overflow-x-auto">
              <table className="table table-zebra table-sm">
                <thead>
                  <tr>
                    <th>{F.id}</th>
                    <th>{F.name}</th>
                    <th>{F.category}</th>
                    <th>{F.health}</th>
                    <th>{F.loyal}</th>
                    <th>{F.chapter}</th>
                    <th>{F.legion}</th>
                  </tr>
                </thead>
                <tbody>
                  {found.map((m) => (
                    <tr key={m.id}>
                      <td>{m.id}</td>
                      <td>{m.name}</td>
                      <td>{m.category}</td>
                      <td>{m.health}</td>
                      <td>{m.loyal ? S.common.yes : S.common.no}</td>
                      <td>{m.chapter?.name ?? S.common.empty}</td>
                      <td>{m.chapter?.parentLegion ?? S.common.empty}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Panel>
      </div>
    </>
  );
}
