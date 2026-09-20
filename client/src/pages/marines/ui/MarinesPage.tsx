import { useEffect, useState } from 'react';
import {
  createMarine,
  deleteMarine,
  getMarine,
  listMarines,
  patchMarine,
  SpaceMarineDetails,
  useChapterOptions,
} from '@/entities/space-marine';
import type { SortableField, SpaceMarineDto, SpaceMarineInputDto, SpaceMarinePageDto } from '@/entities/space-marine';
import { EMPTY_MARINE_FILTERS, MarineFiltersForm } from '@/features/marine-filters';
import type { MarineFilters } from '@/features/marine-filters';
import { diffInput, MarineForm, marineToInput } from '@/features/marine-form';
import type { ErrorDto } from '@/shared/api';
import { S } from '@/shared/config';
import { toggleSortToken, useToast } from '@/shared/lib';
import { ErrorAlert, Modal, PageHeader, Toast } from '@/shared/ui';
import { MarinesTable } from '@/widgets/marines-table';

type ModalState =
  | { kind: 'create' }
  | { kind: 'edit'; marine: SpaceMarineDto }
  | { kind: 'view'; marine: SpaceMarineDto }
  | { kind: 'delete'; marine: SpaceMarineDto }
  | null;

export function MarinesPage() {
  const [filters, setFilters] = useState<MarineFilters>(EMPTY_MARINE_FILTERS);
  const [sort, setSort] = useState<string[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [version, setVersion] = useState(0);

  const [data, setData] = useState<SpaceMarinePageDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<ErrorDto | null>(null);
  const [toast, showToast] = useToast();

  const [modal, setModal] = useState<ModalState>(null);
  const [modalError, setModalError] = useState<ErrorDto | null>(null);
  const [busy, setBusy] = useState(false);

  const options = useChapterOptions(version);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    void listMarines(filters, sort, page, size).then((result) => {
      if (cancelled) return;
      setLoading(false);
      setError(result.ok ? null : result.error);
      if (result.ok) setData(result.value);
    });
    return () => {
      cancelled = true;
    };
  }, [filters, sort, page, size, version]);

  const closeModal = () => {
    setModal(null);
    setModalError(null);
  };

  const succeed = (message: string) => {
    closeModal();
    showToast(message);
    setVersion((value) => value + 1);
  };

  const open = async (id: number) => {
    const result = await getMarine(id);
    if (!result.ok) return setError(result.error);
    setError(null);
    setModal({ kind: 'view', marine: result.value });
  };

  const create = async (input: SpaceMarineInputDto) => {
    setBusy(true);
    const result = await createMarine(input);
    setBusy(false);
    if (result.ok) succeed(S.marine.created(result.value.id));
    else setModalError(result.error);
  };

  const update = async (marine: SpaceMarineDto, input: SpaceMarineInputDto) => {
    const patch = diffInput(marineToInput(marine), input);
    if (Object.keys(patch).length === 0) {
      closeModal();
      return showToast(S.common.noChanges);
    }
    setBusy(true);
    const result = await patchMarine(marine.id, patch);
    setBusy(false);
    if (result.ok) succeed(S.marine.updated(marine.id));
    else setModalError(result.error);
  };

  const remove = async (id: number) => {
    setBusy(true);
    const result = await deleteMarine(id);
    setBusy(false);
    if (result.ok) succeed(S.marine.deleted(id));
    else setModalError(result.error);
  };

  const modalErrorBlock = modalError && (
    <div className="mb-3">
      <ErrorAlert error={modalError} />
    </div>
  );

  return (
    <>
      <PageHeader
        title={S.marine.title}
        subtitle={S.marine.subtitle}
        actions={
          <button type="button" className="btn btn-sm btn-primary" onClick={() => setModal({ kind: 'create' })}>
            {S.common.add}
          </button>
        }
      />

      <MarineFiltersForm
        options={options}
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

      <MarinesTable
        data={data}
        loading={loading}
        sort={sort}
        size={size}
        onToggleSort={(field: SortableField) => {
          setPage(0);
          setSort((current) => toggleSortToken(current, field));
        }}
        onResetSort={() => setSort([])}
        onOpen={(marine) => void open(marine.id)}
        onEdit={(marine) => setModal({ kind: 'edit', marine })}
        onDelete={(marine) => setModal({ kind: 'delete', marine })}
        onPage={setPage}
        onSize={(value) => {
          setSize(value);
          setPage(0);
        }}
      />

      {toast && <Toast message={toast} />}

      <Modal open={modal?.kind === 'create'} title={S.marine.newTitle} onClose={closeModal} wide>
        {modalErrorBlock}
        {modal?.kind === 'create' && (
          <MarineForm
            options={options}
            submitLabel={S.common.create}
            busy={busy}
            onSubmit={(input) => void create(input)}
            onCancel={closeModal}
          />
        )}
      </Modal>

      <Modal
        open={modal?.kind === 'edit'}
        title={modal?.kind === 'edit' ? S.marine.editTitle(modal.marine.id) : ''}
        onClose={closeModal}
        wide
      >
        {modalErrorBlock}
        {modal?.kind === 'edit' && (
          <MarineForm
            initial={modal.marine}
            options={options}
            submitLabel={S.common.save}
            busy={busy}
            onSubmit={(input) => void update(modal.marine, input)}
            onCancel={closeModal}
          />
        )}
      </Modal>

      <Modal
        open={modal?.kind === 'view'}
        title={modal?.kind === 'view' ? S.marine.viewTitle(modal.marine.id) : ''}
        onClose={closeModal}
      >
        {modal?.kind === 'view' && (
          <>
            <SpaceMarineDetails marine={modal.marine} />
            <div className="modal-action">
              <button
                type="button"
                className="btn btn-ghost text-error"
                onClick={() => setModal({ kind: 'delete', marine: modal.marine })}
              >
                {S.common.delete}
              </button>
              <button type="button" className="btn" onClick={() => setModal({ kind: 'edit', marine: modal.marine })}>
                {S.common.edit}
              </button>
              <button type="button" className="btn btn-primary" onClick={closeModal}>
                {S.common.close}
              </button>
            </div>
          </>
        )}
      </Modal>

      <Modal open={modal?.kind === 'delete'} title={S.marine.deleteTitle} onClose={closeModal}>
        {modalErrorBlock}
        {modal?.kind === 'delete' && (
          <>
            <p>{S.marine.deleteText(modal.marine.id, modal.marine.name)}</p>
            <div className="modal-action">
              <button type="button" className="btn" onClick={closeModal} disabled={busy}>
                {S.common.cancel}
              </button>
              <button
                type="button"
                className="btn btn-error"
                onClick={() => void remove(modal.marine.id)}
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
