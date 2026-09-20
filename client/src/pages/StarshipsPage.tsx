import { useEffect, useState } from 'react';
import type { SoaClient } from '../api/client';
import type { ErrorDto, SpaceMarineDto, StarshipDto, StarshipPageDto } from '../api/types';
import { Pagination } from '../components/Pagination';
import { DetailList, EmptyState, ErrorAlert, Modal, PageHeader, SelectField, SortableTh, SortSummary, sortMarkOf, TextField, Toast, toggleSortToken } from '../components/ui';
import { useToast } from '../hooks/useToast';
import { S } from '../strings';

const F = S.ship.fields;

type ModalState =
  | { kind: 'create' }
  | { kind: 'view'; ship: StarshipDto }
  | { kind: 'edit'; ship: StarshipDto }
  | { kind: 'delete'; ship: StarshipDto }
  | null;

/** Корабли: та же раскладка, что и у десантников — таблица, сортировка, страницы, карточка по клику. */
export function StarshipsPage({ client }: { client: SoaClient }) {
  const [sort, setSort] = useState<string[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [version, setVersion] = useState(0);

  const [data, setData] = useState<StarshipPageDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<ErrorDto | null>(null);
  const [toast, showToast] = useToast();

  const [modal, setModal] = useState<ModalState>(null);
  const [modalError, setModalError] = useState<ErrorDto | null>(null);
  const [busy, setBusy] = useState(false);
  const [name, setName] = useState('');
  const [candidate, setCandidate] = useState('');

  /** Десантники первого сервиса — для выбора при посадке и подписей в экипаже. */
  const [marines, setMarines] = useState<SpaceMarineDto[]>([]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    void client.listStarships(sort, page, size).then((r) => {
      if (cancelled) return;
      setLoading(false);
      if (r.ok) {
        setError(null);
        setData(r.value);
      } else {
        setError(r.error);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [client, sort, page, size, version]);

  useEffect(() => {
    let cancelled = false;
    void client.listMarines({}, ['id'], 0, 100).then((r) => {
      if (!cancelled && r.ok) setMarines(r.value.items);
    });
    return () => {
      cancelled = true;
    };
  }, [client, version]);

  const refresh = () => setVersion((v) => v + 1);

  const closeModal = () => {
    setModal(null);
    setModalError(null);
  };

  const marineLabel = (id: number) => {
    const m = marines.find((x) => x.id === id);
    return m ? `№${id} · ${m.name}` : `№${id}`;
  };

  /** Карточка открывается отдельным запросом по id. */
  const open = async (id: number) => {
    const r = await client.getStarship(id);
    if (r.ok) {
      setError(null);
      setModalError(null);
      setCandidate('');
      setModal({ kind: 'view', ship: r.value });
    } else {
      setError(r.error);
    }
  };

  const create = async () => {
    setBusy(true);
    const r = await client.createStarship(name);
    setBusy(false);
    if (r.ok) {
      closeModal();
      showToast(S.ship.created(r.value.id));
      refresh();
    } else {
      setModalError(r.error);
    }
  };

  const rename = async (ship: StarshipDto) => {
    if (name === ship.name) {
      closeModal();
      showToast(S.common.noChanges);
      return;
    }
    setBusy(true);
    const r = await client.renameStarship(ship.id, name);
    setBusy(false);
    if (r.ok) {
      showToast(S.ship.renamed);
      refresh();
      setModalError(null);
      setModal({ kind: 'view', ship: r.value });
    } else {
      setModalError(r.error);
    }
  };

  const remove = async (id: number) => {
    setBusy(true);
    const r = await client.deleteStarship(id);
    setBusy(false);
    if (r.ok) {
      closeModal();
      showToast(S.ship.deleted(id));
      refresh();
    } else {
      setModalError(r.error);
    }
  };

  const board = async (ship: StarshipDto) => {
    const marineId = Number(candidate);
    setBusy(true);
    const r = await client.boardMarine(ship.id, marineId);
    setBusy(false);
    if (r.ok) {
      showToast(S.ship.boarded(marineId, ship.id));
      refresh();
      setModalError(null);
      setCandidate('');
      setModal({ kind: 'view', ship: r.value });
    } else {
      setModalError(r.error);
    }
  };

  const unload = async (ship: StarshipDto, marineId: number) => {
    setBusy(true);
    const r = await client.unloadMarine(ship.id, marineId);
    setBusy(false);
    if (r.ok) {
      showToast(r.value.message);
      refresh();
      const fresh = await client.getStarship(ship.id);
      setModalError(null);
      if (fresh.ok) setModal({ kind: 'view', ship: fresh.value });
      else closeModal();
    } else {
      setModalError(r.error);
    }
  };

  const toggleSort = (field: 'id' | 'name') => {
    setPage(0);
    setSort((cur) => toggleSortToken(cur, field));
  };

  const modalErrorBlock = modalError && (
    <div className="mb-3">
      <ErrorAlert error={modalError} />
    </div>
  );

  const freeMarines = (ship: StarshipDto) => marines.filter((m) => !ship.marines.includes(m.id));

  return (
    <>
      <PageHeader
        title={S.ship.title}
        subtitle={S.ship.subtitle}
        actions={
          <button
            type="button"
            className="btn btn-sm btn-primary"
            onClick={() => {
              setName('');
              setModal({ kind: 'create' });
            }}
          >
            {S.common.add}
          </button>
        }
      />

      {error && (
        <div className="mb-4">
          <ErrorAlert error={error} onClose={() => setError(null)} />
        </div>
      )}

      <div className="card bg-base-100 shadow-sm">
        <div className="card-body gap-2 p-0">
          <SortSummary sort={sort} loading={loading} onReset={() => setSort([])} />

          <div className="overflow-x-auto">
            <table className="table table-zebra table-sm">
              <thead>
                <tr>
                  <SortableTh title={F.id} mark={sortMarkOf(sort, 'id')} onClick={() => toggleSort('id')} />
                  <SortableTh title={F.name} mark={sortMarkOf(sort, 'name')} onClick={() => toggleSort('name')} />
                  <th>{F.crew}</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {data?.items.map((s) => (
                  <tr key={s.id} className="hover cursor-pointer" onClick={() => void open(s.id)}>
                    <td>{s.id}</td>
                    <td>{s.name}</td>
                    <td>{S.ship.crewSize(s.marines.length)}</td>
                    <td className="whitespace-nowrap text-right" onClick={(e) => e.stopPropagation()}>
                      <button
                        type="button"
                        className="btn btn-ghost btn-xs"
                        onClick={() => {
                          setName(s.name);
                          setModal({ kind: 'edit', ship: s });
                        }}
                      >
                        {S.common.edit}
                      </button>
                      <button type="button" className="btn btn-ghost btn-xs text-error" onClick={() => setModal({ kind: 'delete', ship: s })}>
                        {S.common.delete}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {data && data.items.length === 0 && <EmptyState text={S.common.nothingFound} />}
            {!data && !error && <EmptyState text={S.common.loading} />}
          </div>

          {data && (
            <div className="px-4 pb-4">
              <Pagination
                page={data.page}
                totalPages={data.totalPages}
                totalElements={data.totalElements}
                size={size}
                onPage={setPage}
                onSize={(s) => {
                  setSize(s);
                  setPage(0);
                }}
              />
            </div>
          )}
        </div>
      </div>

      {toast && <Toast message={toast} />}

      <Modal open={modal?.kind === 'create'} title={S.ship.newTitle} onClose={closeModal}>
        {modalErrorBlock}
        {modal?.kind === 'create' && (
          <form
            onSubmit={(e) => {
              e.preventDefault();
              void create();
            }}
          >
            <TextField label={F.name} value={name} onChange={setName} autoFocus />
            <div className="modal-action">
              <button type="button" className="btn" onClick={closeModal} disabled={busy}>
                {S.common.cancel}
              </button>
              <button type="submit" className="btn btn-primary" disabled={busy}>
                {busy && <span className="loading loading-spinner loading-xs" />}
                {S.common.create}
              </button>
            </div>
          </form>
        )}
      </Modal>

      <Modal open={modal?.kind === 'edit'} title={modal?.kind === 'edit' ? S.ship.viewTitle(modal.ship.id) : ''} onClose={closeModal}>
        {modalErrorBlock}
        {modal?.kind === 'edit' && (
          <form
            onSubmit={(e) => {
              e.preventDefault();
              void rename(modal.ship);
            }}
          >
            <TextField label={F.name} value={name} onChange={setName} autoFocus />
            <div className="modal-action">
              <button type="button" className="btn" onClick={closeModal} disabled={busy}>
                {S.common.cancel}
              </button>
              <button type="submit" className="btn btn-primary" disabled={busy}>
                {busy && <span className="loading loading-spinner loading-xs" />}
                {S.common.save}
              </button>
            </div>
          </form>
        )}
      </Modal>

      <Modal open={modal?.kind === 'view'} title={modal?.kind === 'view' ? S.ship.viewTitle(modal.ship.id) : ''} onClose={closeModal}>
        {modalErrorBlock}
        {modal?.kind === 'view' && (
          <>
            <DetailList
              items={[
                { label: F.id, value: modal.ship.id },
                { label: F.name, value: modal.ship.name },
                { label: F.crew, value: modal.ship.marines.length },
              ]}
            />

            <div className="divider my-3 text-sm">{S.ship.crew}</div>
            {modal.ship.marines.length === 0 ? (
              <p className="text-sm opacity-60">{S.ship.noCrew}</p>
            ) : (
              <ul className="menu menu-sm w-full rounded-box bg-base-200 p-1">
                {modal.ship.marines.map((id) => (
                  <li key={id}>
                    <div className="flex justify-between">
                      <span>{marineLabel(id)}</span>
                      <button type="button" className="btn btn-ghost btn-xs" disabled={busy} onClick={() => void unload(modal.ship, id)}>
                        {S.ship.unload}
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}

            <form
              className="mt-3 flex items-end gap-2"
              onSubmit={(e) => {
                e.preventDefault();
                void board(modal.ship);
              }}
            >
              <div className="grow">
                <SelectField
                  label={S.ship.marineToBoard}
                  value={candidate}
                  options={[{ value: '', label: S.common.empty }, ...freeMarines(modal.ship).map((m) => ({ value: String(m.id), label: marineLabel(m.id) }))]}
                  onChange={setCandidate}
                />
              </div>
              <button type="submit" className="btn btn-sm btn-primary mb-1" disabled={busy || candidate === ''}>
                {S.ship.board}
              </button>
            </form>

            <div className="modal-action">
              <button type="button" className="btn btn-ghost text-error" onClick={() => setModal({ kind: 'delete', ship: modal.ship })}>
                {S.common.delete}
              </button>
              <button
                type="button"
                className="btn"
                onClick={() => {
                  setName(modal.ship.name);
                  setModal({ kind: 'edit', ship: modal.ship });
                }}
              >
                {S.common.edit}
              </button>
              <button type="button" className="btn btn-primary" onClick={closeModal}>
                {S.common.close}
              </button>
            </div>
          </>
        )}
      </Modal>

      <Modal open={modal?.kind === 'delete'} title={S.ship.deleteTitle} onClose={closeModal}>
        {modalErrorBlock}
        {modal?.kind === 'delete' && (
          <>
            <p>{S.ship.deleteText(modal.ship.id, modal.ship.name)}</p>
            <div className="modal-action">
              <button type="button" className="btn" onClick={closeModal} disabled={busy}>
                {S.common.cancel}
              </button>
              <button type="button" className="btn btn-error" onClick={() => void remove(modal.ship.id)} disabled={busy}>
                {busy && <span className="loading loading-spinner loading-xs" />}
                {S.common.delete}
              </button>
            </div>
          </>
        )}
      </Modal>
    </>
  );
}
