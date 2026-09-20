import { useEffect, useState } from 'react';
import { findMarinesByNamePrefix } from '@/entities/space-marine';
import type { SpaceMarinePageDto } from '@/entities/space-marine';
import type { ErrorDto } from '@/shared/api';
import { S } from '@/shared/config';
import { EmptyState, ErrorAlert, Pagination, Panel, TextField } from '@/shared/ui';

const F = S.marine.fields;

/** Десантники, имя которых начинается с заданной подстроки. Результат постраничный. */
export function SearchMarinesByName() {
  const [prefix, setPrefix] = useState('');
  /** Отправленный префикс: страницы листаются по нему, а не по тому, что сейчас в поле. */
  const [applied, setApplied] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [found, setFound] = useState<SpaceMarinePageDto | null>(null);
  const [error, setError] = useState<ErrorDto | null>(null);

  useEffect(() => {
    if (applied === null) return;
    let cancelled = false;
    void findMarinesByNamePrefix(applied, page, size).then((result) => {
      if (cancelled) return;
      setError(result.ok ? null : result.error);
      setFound(result.ok ? result.value : null);
    });
    return () => {
      cancelled = true;
    };
  }, [applied, page, size]);

  return (
    <Panel title={S.extras.namePrefix} className="xl:col-span-2">
      <form
        className="flex flex-wrap items-end gap-3"
        onSubmit={(event) => {
          event.preventDefault();
          setPage(0);
          setApplied(prefix);
        }}
      >
        <div className="w-72">
          <TextField label={S.extras.prefix} value={prefix} onChange={setPrefix} />
        </div>
        <button type="submit" className="btn btn-sm btn-primary mb-1">
          {S.extras.find}
        </button>
      </form>

      {error && <ErrorAlert error={error} onClose={() => setError(null)} />}
      {found && found.items.length === 0 && <EmptyState text={S.common.nothingFound} />}
      {found && found.items.length > 0 && (
        <>
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
                {found.items.map((marine) => (
                  <tr key={marine.id}>
                    <td>{marine.id}</td>
                    <td>{marine.name}</td>
                    <td>{marine.category}</td>
                    <td>{marine.health}</td>
                    <td>{marine.loyal ? S.common.yes : S.common.no}</td>
                    <td>{marine.chapter?.name ?? S.common.empty}</td>
                    <td>{marine.chapter?.parentLegion ?? S.common.empty}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination
            page={found.page}
            totalPages={found.totalPages}
            totalElements={found.totalElements}
            size={size}
            onPage={setPage}
            onSize={(value) => {
              setSize(value);
              setPage(0);
            }}
          />
        </>
      )}
    </Panel>
  );
}
