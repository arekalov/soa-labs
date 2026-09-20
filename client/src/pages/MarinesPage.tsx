import { useEffect, useState } from 'react';
import type { SoaClient } from '../api/client';
import { CATEGORIES, SORTABLE_FIELDS, type ErrorDto, type SortableField, type SpaceMarineDto, type SpaceMarineInputDto, type SpaceMarinePageDto } from '../api/types';
import { diffInput, MarineForm, marineToInput } from '../components/MarineForm';
import { Pagination } from '../components/Pagination';
import { DetailList, EmptyState, ErrorAlert, Modal, PageHeader, SelectField, SortableTh, SortSummary, sortMarkOf, TextField, Toast, toggleSortToken } from '../components/ui';
import { useChapterOptions } from '../hooks/useChapterOptions';
import { useToast } from '../hooks/useToast';
import { S } from '../strings';

type Filters = Record<SortableField, string>;
const EMPTY_FILTERS = Object.fromEntries(SORTABLE_FIELDS.map((f) => [f, ''])) as Filters;

const F = S.marine.fields;

interface Column {
  field: SortableField;
  title: string;
  render: (m: SpaceMarineDto) => string;
}

/** Колонки соответствуют полям спецификации один к одному — по каждой можно и фильтровать, и сортировать. */
const COLUMNS: Column[] = [
  { field: 'id', title: F.id, render: (m) => String(m.id) },
  { field: 'name', title: F.name, render: (m) => m.name },
  { field: 'category', title: F.category, render: (m) => m.category },
  { field: 'health', title: F.health, render: (m) => String(m.health) },
  { field: 'loyal', title: F.loyal, render: (m) => (m.loyal ? S.common.yes : S.common.no) },
  { field: 'coordinatesX', title: 'X', render: (m) => String(m.coordinates.x ?? S.common.empty) },
  { field: 'coordinatesY', title: 'Y', render: (m) => String(m.coordinates.y ?? S.common.empty) },
  { field: 'chapterName', title: F.chapter, render: (m) => m.chapter?.name ?? S.common.empty },
  { field: 'chapterParentLegion', title: F.legion, render: (m) => m.chapter?.parentLegion ?? S.common.empty },
  { field: 'achievements', title: F.achievements, render: (m) => m.achievements ?? S.common.empty },
  { field: 'creationDate', title: F.created, render: (m) => formatDate(m.creationDate) },
];

export function formatDate(iso: string): string {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString('ru-RU');
}

/** Карточка десантника: подпись — значение. */
export function MarineDetails({ marine: m }: { marine: SpaceMarineDto }) {
  return (
    <DetailList
      items={[
        { label: F.id, value: m.id },
        { label: F.name, value: m.name },
        { label: F.category, value: m.category },
        { label: F.health, value: m.health },
        { label: F.loyal, value: m.loyal ? S.common.yes : S.common.no },
        { label: F.coordinates, value: `(${m.coordinates.x ?? S.common.empty}; ${m.coordinates.y ?? S.common.empty})` },
        { label: F.chapter, value: m.chapter?.name ?? S.common.empty },
        { label: F.legion, value: m.chapter?.parentLegion ?? S.common.empty },
        { label: F.achievements, value: m.achievements ?? S.common.empty },
        { label: F.created, value: formatDate(m.creationDate) },
      ]}
    />
  );
}

type ModalState =
  | { kind: 'create' }
  | { kind: 'edit'; marine: SpaceMarineDto }
  | { kind: 'view'; marine: SpaceMarineDto }
  | { kind: 'delete'; marine: SpaceMarineDto }
  | null;

export function MarinesPage({ client }: { client: SoaClient }) {
  const [draft, setDraft] = useState<Filters>(EMPTY_FILTERS);
  const [applied, setApplied] = useState<Filters>(EMPTY_FILTERS);
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

  const options = useChapterOptions(client, version);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    void client.listMarines(applied, sort, page, size).then((r) => {
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
  }, [client, applied, sort, page, size, version]);

  const refresh = () => setVersion((v) => v + 1);

  const closeModal = () => {
    setModal(null);
    setModalError(null);
  };

  const succeed = (message: string) => {
    closeModal();
    showToast(message);
    refresh();
  };

  const toggleSort = (field: SortableField) => {
    setPage(0);
    setSort((cur) => toggleSortToken(cur, field));
  };

  const applyFilters = () => {
    setApplied(draft);
    setPage(0);
  };

  const resetFilters = () => {
    setDraft(EMPTY_FILTERS);
    setApplied(EMPTY_FILTERS);
    setPage(0);
  };

  const activeFilters = Object.values(applied).filter((v) => v.trim() !== '').length;

  /** Открытие карточки — отдельный запрос по id, а не данные из строки таблицы. */
  const open = async (id: number) => {
    const r = await client.getMarine(id);
    if (r.ok) {
      setError(null);
      setModal({ kind: 'view', marine: r.value });
    } else {
      setError(r.error);
    }
  };

  const create = async (input: SpaceMarineInputDto) => {
    setBusy(true);
    const r = await client.createMarine(input);
    setBusy(false);
    if (r.ok) succeed(S.marine.created(r.value.id));
    else setModalError(r.error);
  };

  /** Изменение — это PATCH с разницей между исходным объектом и формой. */
  const update = async (marine: SpaceMarineDto, input: SpaceMarineInputDto) => {
    const patch = diffInput(marineToInput(marine), input);
    if (Object.keys(patch).length === 0) {
      closeModal();
      showToast(S.common.noChanges);
      return;
    }
    setBusy(true);
    const r = await client.patchMarine(marine.id, patch);
    setBusy(false);
    if (r.ok) succeed(S.marine.updated(marine.id));
    else setModalError(r.error);
  };

  const remove = async (id: number) => {
    setBusy(true);
    const r = await client.deleteMarine(id);
    setBusy(false);
    if (r.ok) succeed(S.marine.deleted(id));
    else setModalError(r.error);
  };

  const setF = (field: SortableField) => (v: string) => setDraft({ ...draft, [field]: v });

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

      <div className="collapse collapse-arrow mb-4 bg-base-100 shadow-sm">
        <input type="checkbox" defaultChecked />
        <div className="collapse-title flex items-center gap-2 font-semibold">
          {S.common.filters}
          {activeFilters > 0 && <span className="badge badge-primary badge-sm">{activeFilters}</span>}
        </div>
        <div className="collapse-content">
          <div className="grid grid-cols-2 gap-x-4 md:grid-cols-4">
            <TextField label={F.id} value={draft.id} onChange={setF('id')} />
            <TextField label={F.name} value={draft.name} onChange={setF('name')} />
            <SelectField
              label={F.category}
              value={draft.category}
              options={[{ value: '', label: S.common.any }, ...CATEGORIES.map((c) => ({ value: c, label: c }))]}
              onChange={setF('category')}
            />
            <SelectField
              label={F.loyal}
              value={draft.loyal}
              options={[
                { value: '', label: S.common.anyone },
                { value: 'true', label: S.common.yes },
                { value: 'false', label: S.common.no },
              ]}
              onChange={setF('loyal')}
            />
            <TextField label={F.health} value={draft.health} onChange={setF('health')} />
            <TextField label={F.x} value={draft.coordinatesX} onChange={setF('coordinatesX')} />
            <TextField label={F.y} value={draft.coordinatesY} onChange={setF('coordinatesY')} />
            <TextField label={F.created} value={draft.creationDate} onChange={setF('creationDate')} placeholder="2026-09-19T10:26:46.736Z" />
            <TextField label={F.chapter} value={draft.chapterName} onChange={setF('chapterName')} options={options.chapters} />
            <TextField label={F.legion} value={draft.chapterParentLegion} onChange={setF('chapterParentLegion')} options={options.legions} />
            <TextField label={F.achievements} value={draft.achievements} onChange={setF('achievements')} />
          </div>
          <div className="mt-2 flex flex-wrap items-center gap-2">
            <button type="button" className="btn btn-sm btn-primary" onClick={applyFilters}>
              {S.common.apply}
            </button>
            <button type="button" className="btn btn-sm btn-ghost" onClick={resetFilters}>
              {S.common.reset}
            </button>
          </div>
        </div>
      </div>

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
                  {COLUMNS.map((c) => (
                    <SortableTh key={c.field} title={c.title} mark={sortMarkOf(sort, c.field)} onClick={() => toggleSort(c.field)} />
                  ))}
                  <th />
                </tr>
              </thead>
              <tbody>
                {data?.items.map((m) => (
                  <tr key={m.id} className="hover cursor-pointer" onClick={() => void open(m.id)}>
                    {COLUMNS.map((c) => (
                      <td key={c.field} className="max-w-56 truncate">
                        {c.render(m)}
                      </td>
                    ))}
                    <td className="whitespace-nowrap text-right" onClick={(e) => e.stopPropagation()}>
                      <button type="button" className="btn btn-ghost btn-xs" onClick={() => setModal({ kind: 'edit', marine: m })}>
                        {S.common.edit}
                      </button>
                      <button type="button" className="btn btn-ghost btn-xs text-error" onClick={() => setModal({ kind: 'delete', marine: m })}>
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

      <Modal open={modal?.kind === 'create'} title={S.marine.newTitle} onClose={closeModal} wide>
        {modalErrorBlock}
        {modal?.kind === 'create' && (
          <MarineForm options={options} submitLabel={S.common.create} busy={busy} onSubmit={(input) => void create(input)} onCancel={closeModal} />
        )}
      </Modal>

      <Modal open={modal?.kind === 'edit'} title={modal?.kind === 'edit' ? S.marine.editTitle(modal.marine.id) : ''} onClose={closeModal} wide>
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

      <Modal open={modal?.kind === 'view'} title={modal?.kind === 'view' ? S.marine.viewTitle(modal.marine.id) : ''} onClose={closeModal}>
        {modal?.kind === 'view' && (
          <>
            <MarineDetails marine={modal.marine} />
            <div className="modal-action">
              <button type="button" className="btn btn-ghost text-error" onClick={() => setModal({ kind: 'delete', marine: modal.marine })}>
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
              <button type="button" className="btn btn-error" onClick={() => void remove(modal.marine.id)} disabled={busy}>
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
