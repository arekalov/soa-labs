import { useEffect, useState } from 'react';
import { listMarines } from '@/entities/space-marine';
import type { SpaceMarineDto } from '@/entities/space-marine';
import {
  createStarship,
  deleteStarship,
  getStarship,
  listStarships,
  renameStarship,
  StarshipDetails,
} from '@/entities/starship';
import type { StarshipDto, StarshipPageDto } from '@/entities/starship';
import { StarshipCrew } from '@/features/starship-crew';
import { EMPTY_STARSHIP_FILTERS, StarshipFiltersForm } from '@/features/starship-filters';
import type { StarshipFilters } from '@/features/starship-filters';
import type { ErrorDto } from '@/shared/api';
import { S } from '@/shared/config';
import { toggleSortToken, useToast } from '@/shared/lib';
import { ErrorAlert, Modal, PageHeader, TextField, Toast } from '@/shared/ui';
import { StarshipsTable } from '@/widgets/starships-table';

const MARINES_SAMPLE = 100;

type ModalState =
  | { kind: 'create' }
  | { kind: 'view'; starship: StarshipDto }
  | { kind: 'edit'; starship: StarshipDto }
  | { kind: 'delete'; starship: StarshipDto }
  | null;

/** Раскладка та же, что у десантников: фильтры, таблица, страницы, карточка по клику. */
export function StarshipsPage() {
  const [filters, setFilters] = useState<StarshipFilters>(EMPTY_STARSHIP_FILTERS);
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

  const [marines, setMarines] = useState<SpaceMarineDto[]>([]);
  const [boarded, setBoarded] = useState<Set<number>>(new Set());

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    void listStarships(filters, sort, page, size).then((result) => {
      if (cancelled) return;
      setLoading(false);
      setError(result.ok ? null : result.error);
      if (result.ok) setData(result.value);
    });
    return () => {
      cancelled = true;
    };
  }, [filters, sort, page, size, version]);

  useEffect(() => {
    let cancelled = false;
    void listMarines({}, ['id'], 0, MARINES_SAMPLE).then((result) => {
      if (!cancelled && result.ok) setMarines(result.value.items);
    });
    void listStarships({}, [], 0, MARINES_SAMPLE).then((result) => {
      if (!cancelled && result.ok) setBoarded(new Set(result.value.items.flatMap((ship) => ship.marines)));
    });
    return () => {
      cancelled = true;
    };
  }, [version]);

  const refresh = () => setVersion((value) => value + 1);

  const closeModal = () => {
    setModal(null);
    setModalError(null);
  };

  const open = async (id: number) => {
    const result = await getStarship(id);
    if (!result.ok) return setError(result.error);
    setError(null);
    setModalError(null);
    setModal({ kind: 'view', starship: result.value });
  };

  const create = async () => {
    setBusy(true);
    const result = await createStarship(name);
    setBusy(false);
    if (!result.ok) return setModalError(result.error);
    closeModal();
    showToast(S.ship.created(result.value.id));
    refresh();
  };

  const rename = async (starship: StarshipDto) => {
    if (name === starship.name) {
      closeModal();
      return showToast(S.common.noChanges);
    }
    setBusy(true);
    const result = await renameStarship(starship.id, name);
    setBusy(false);
    if (!result.ok) return setModalError(result.error);
    showToast(S.ship.renamed);
    refresh();
    setModalError(null);
    setModal({ kind: 'view', starship: result.value });
  };

  const remove = async (id: number) => {
    setBusy(true);
    const result = await deleteStarship(id);
    setBusy(false);
    if (!result.ok) return setModalError(result.error);
    closeModal();
    showToast(S.ship.deleted(id));
    refresh();
  };

  const startEdit = (starship: StarshipDto) => {
    setName(starship.name);
    setModal({ kind: 'edit', starship });
  };

  const modalErrorBlock = modalError && (
    <div className="mb-3">
      <ErrorAlert error={modalError} />
    </div>
  );

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

      <StarshipFiltersForm
        onChange={(next) => {
          setFilters(next);
          setPage(0);
        }}
      />

      {error && (
        <div className="mb-4">
          <ErrorAlert error={error} onClose={() => setError(null)} />
        </div>
      )}

      <StarshipsTable
        data={data}
        loading={loading}
        sort={sort}
        size={size}
        onToggleSort={(field) => {
          setPage(0);
          setSort((current) => toggleSortToken(current, field));
        }}
        onResetSort={() => setSort([])}
        onOpen={(starship) => void open(starship.id)}
        onEdit={startEdit}
        onDelete={(starship) => setModal({ kind: 'delete', starship })}
        onPage={setPage}
        onSize={(value) => {
          setSize(value);
          setPage(0);
        }}
      />

      {toast && <Toast message={toast} />}

      <Modal open={modal?.kind === 'create'} title={S.ship.newTitle} onClose={closeModal}>
        {modalErrorBlock}
        {modal?.kind === 'create' && (
          <form
            onSubmit={(event) => {
              event.preventDefault();
              void create();
            }}
          >
            <TextField label={S.ship.fields.name} value={name} onChange={setName} autoFocus />
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

      <Modal
        open={modal?.kind === 'edit'}
        title={modal?.kind === 'edit' ? S.ship.editTitle(modal.starship.id) : ''}
        onClose={closeModal}
      >
        {modalErrorBlock}
        {modal?.kind === 'edit' && (
          <form
            onSubmit={(event) => {
              event.preventDefault();
              void rename(modal.starship);
            }}
          >
            <TextField label={S.ship.fields.name} value={name} onChange={setName} autoFocus />
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

      <Modal
        open={modal?.kind === 'view'}
        title={modal?.kind === 'view' ? S.ship.viewTitle(modal.starship.id) : ''}
        onClose={closeModal}
      >
        {modalErrorBlock}
        {modal?.kind === 'view' && (
          <>
            <StarshipDetails starship={modal.starship} />
            <StarshipCrew
              starship={modal.starship}
              marines={marines}
              boarded={boarded}
              onChanged={(starship) => {
                setModal({ kind: 'view', starship });
                refresh();
              }}
              onMessage={showToast}
              onError={setModalError}
            />
            <div className="modal-action">
              <button
                type="button"
                className="btn btn-ghost text-error"
                onClick={() => setModal({ kind: 'delete', starship: modal.starship })}
              >
                {S.common.delete}
              </button>
              <button type="button" className="btn" onClick={() => startEdit(modal.starship)}>
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
            <p>{S.ship.deleteText(modal.starship.id, modal.starship.name)}</p>
            <div className="modal-action">
              <button type="button" className="btn" onClick={closeModal} disabled={busy}>
                {S.common.cancel}
              </button>
              <button
                type="button"
                className="btn btn-error"
                onClick={() => void remove(modal.starship.id)}
                disabled={busy}
              >
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
