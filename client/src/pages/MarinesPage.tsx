import { useEffect, useState } from 'react';
import type { SoaClient } from '../api/client';
import { CATEGORIES, SORTABLE_FIELDS, type ErrorDto, type SortableField, type SpaceMarineDto, type SpaceMarineInputDto, type SpaceMarinePageDto } from '../api/types';
import { MarineForm } from '../components/MarineForm';
import { Pagination } from '../components/Pagination';
import { EmptyState, ErrorAlert, Hint, Modal, PageHeader, SelectField, TextArea, TextField, Toast } from '../components/ui';

type Filters = Record<SortableField, string>;
const EMPTY_FILTERS = Object.fromEntries(SORTABLE_FIELDS.map((f) => [f, ''])) as Filters;

interface Column {
  field: SortableField;
  title: string;
  render: (m: SpaceMarineDto) => string;
}

/** Колонки соответствуют полям спецификации один к одному — по каждой можно и фильтровать, и сортировать. */
const COLUMNS: Column[] = [
  { field: 'id', title: 'ID', render: (m) => String(m.id) },
  { field: 'name', title: 'Имя', render: (m) => m.name },
  { field: 'category', title: 'Категория', render: (m) => m.category },
  { field: 'health', title: 'Здоровье', render: (m) => String(m.health) },
  { field: 'loyal', title: 'Верен', render: (m) => (m.loyal ? 'да' : 'нет') },
  { field: 'coordinatesX', title: 'X', render: (m) => String(m.coordinates.x ?? '—') },
  { field: 'coordinatesY', title: 'Y', render: (m) => String(m.coordinates.y ?? '—') },
  { field: 'chapterName', title: 'Орден', render: (m) => m.chapter?.name ?? '—' },
  { field: 'chapterParentLegion', title: 'Легион', render: (m) => m.chapter?.parentLegion ?? '—' },
  { field: 'achievements', title: 'Достижения', render: (m) => m.achievements ?? '—' },
  { field: 'creationDate', title: 'Создан', render: (m) => formatDate(m.creationDate) },
];

function formatDate(iso: string): string {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString('ru-RU');
}

/** Человеко-читаемое описание — абзац текста, как требует задание. */
function describe(m: SpaceMarineDto): string {
  const chapter = m.chapter ? `Орден — ${m.chapter.name}${m.chapter.parentLegion ? `, легион ${m.chapter.parentLegion}` : ''}.` : 'Орден не указан.';
  const achievements = m.achievements ? `Достижения: ${m.achievements}.` : 'Достижений нет.';
  return (
    `Десантник №${m.id} по имени ${m.name}, категория ${m.category}, здоровье ${m.health}, ` +
    `${m.loyal ? 'верен Империуму' : 'не верен Империуму'}. Находится в точке (${m.coordinates.x}, ${m.coordinates.y}). ` +
    `Создан ${formatDate(m.creationDate)}. ${chapter} ${achievements}`
  );
}

type ModalState =
  | { kind: 'create' }
  | { kind: 'edit'; marine: SpaceMarineDto }
  | { kind: 'patch'; marine: SpaceMarineDto }
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
  const [toast, setToast] = useState<string | null>(null);

  const [modal, setModal] = useState<ModalState>(null);
  const [modalError, setModalError] = useState<ErrorDto | null>(null);
  const [busy, setBusy] = useState(false);
  const [patchBody, setPatchBody] = useState('{\n  "achievements": null\n}');
  const [lookupId, setLookupId] = useState('');

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

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 3500);
    return () => clearTimeout(t);
  }, [toast]);

  const refresh = () => setVersion((v) => v + 1);

  const closeModal = () => {
    setModal(null);
    setModalError(null);
  };

  const succeed = (message: string) => {
    closeModal();
    setToast(message);
    refresh();
  };

  /** Клик по заголовку: по возрастанию → по убыванию → без сортировки. Порядок кликов задаёт приоритет. */
  const toggleSort = (field: SortableField) => {
    setPage(0);
    setSort((cur) => {
      if (cur.includes(field)) return [...cur.filter((t) => t !== field), `-${field}`];
      if (cur.includes(`-${field}`)) return cur.filter((t) => t !== `-${field}`);
      return [...cur, field];
    });
  };

  const sortMark = (field: SortableField) => {
    const i = sort.findIndex((t) => t === field || t === `-${field}`);
    if (i < 0) return null;
    return { dir: sort[i] === field ? '↑' : '↓', priority: i + 1 };
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

  const lookup = async () => {
    if (lookupId.trim() === '') return;
    const r = await client.getMarine(lookupId.trim());
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
    if (r.ok) succeed(`Десантник создан, присвоен идентификатор ${r.value.id}.`);
    else setModalError(r.error);
  };

  const replace = async (id: number, input: SpaceMarineInputDto) => {
    setBusy(true);
    const r = await client.replaceMarine(String(id), input);
    setBusy(false);
    if (r.ok) succeed(`Десантник №${id} полностью заменён.`);
    else setModalError(r.error);
  };

  const patch = async (id: number) => {
    setBusy(true);
    const r = await client.patchMarine(String(id), patchBody);
    setBusy(false);
    if (r.ok) succeed(`Десантник №${id} обновлён.`);
    else setModalError(r.error);
  };

  const remove = async (id: number) => {
    setBusy(true);
    const r = await client.deleteMarine(String(id));
    setBusy(false);
    if (r.ok) succeed(`Десантник №${id} удалён.`);
    else setModalError(r.error);
  };

  const setF = (field: SortableField) => (v: string) => setDraft({ ...draft, [field]: v });

  return (
    <>
      <PageHeader
        title="Десантники"
        subtitle="Коллекция объектов SpaceMarine: фильтрация по любому полю, многоступенчатая сортировка, постраничный вывод"
        actions={
          <>
            <form
              className="join"
              onSubmit={(e) => {
                e.preventDefault();
                void lookup();
              }}
            >
              <input className="input input-sm join-item w-28" placeholder="id" value={lookupId} onChange={(e) => setLookupId(e.target.value)} />
              <button type="submit" className="btn btn-sm join-item">
                Найти по id
              </button>
            </form>
            <button type="button" className="btn btn-sm btn-primary" onClick={() => setModal({ kind: 'create' })}>
              + Добавить
            </button>
          </>
        }
      />

      <div className="collapse collapse-arrow mb-4 bg-base-100 shadow-sm">
        <input type="checkbox" defaultChecked />
        <div className="collapse-title flex items-center gap-2 font-semibold">
          Фильтры
          {activeFilters > 0 && <span className="badge badge-primary badge-sm">{activeFilters}</span>}
        </div>
        <div className="collapse-content">
          <div className="grid grid-cols-2 gap-x-4 md:grid-cols-4">
            <TextField label="ID" value={draft.id} onChange={setF('id')} />
            <TextField label="Имя" value={draft.name} onChange={setF('name')} />
            <SelectField
              label="Категория"
              value={draft.category}
              options={[{ value: '', label: 'любая' }, ...CATEGORIES.map((c) => ({ value: c, label: c }))]}
              onChange={setF('category')}
            />
            <SelectField
              label="Верен Империуму"
              value={draft.loyal}
              options={[
                { value: '', label: 'не важно' },
                { value: 'true', label: 'да' },
                { value: 'false', label: 'нет' },
              ]}
              onChange={setF('loyal')}
            />
            <TextField label="Здоровье" value={draft.health} onChange={setF('health')} />
            <TextField label="Координата X" value={draft.coordinatesX} onChange={setF('coordinatesX')} />
            <TextField label="Координата Y" value={draft.coordinatesY} onChange={setF('coordinatesY')} />
            <TextField label="Создан" value={draft.creationDate} onChange={setF('creationDate')} placeholder="2026-09-19T10:26:46.736Z" />
            <TextField label="Орден" value={draft.chapterName} onChange={setF('chapterName')} />
            <TextField label="Легион ордена" value={draft.chapterParentLegion} onChange={setF('chapterParentLegion')} />
            <TextField label="Достижения" value={draft.achievements} onChange={setF('achievements')} />
          </div>
          <div className="mt-2 flex flex-wrap items-center gap-2">
            <button type="button" className="btn btn-sm btn-primary" onClick={applyFilters}>
              Применить
            </button>
            <button type="button" className="btn btn-sm btn-ghost" onClick={resetFilters}>
              Сбросить
            </button>
            <Hint>Сравнение точное, условия объединяются по «И». Пустые поля не отправляются.</Hint>
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
          <div className="flex flex-wrap items-center gap-2 px-4 pt-3 text-sm">
            {sort.length === 0 ? (
              <span className="opacity-60">Сортировка: по возрастанию id (по умолчанию). Нажмите на заголовок колонки.</span>
            ) : (
              <>
                <span className="opacity-60">Сортировка:</span>
                {sort.map((t) => (
                  <span key={t} className="badge badge-outline badge-sm">
                    {t.startsWith('-') ? `${t.slice(1)} ↓` : `${t} ↑`}
                  </span>
                ))}
                <button type="button" className="btn btn-ghost btn-xs" onClick={() => setSort([])}>
                  сбросить
                </button>
              </>
            )}
            {loading && <span className="loading loading-spinner loading-xs ml-auto" />}
          </div>

          <div className="overflow-x-auto">
            <table className="table table-zebra table-sm">
              <thead>
                <tr>
                  {COLUMNS.map((c) => {
                    const mark = sortMark(c.field);
                    return (
                      <th key={c.field} className="cursor-pointer select-none whitespace-nowrap hover:bg-base-200" onClick={() => toggleSort(c.field)}>
                        {c.title}
                        {mark && (
                          <span className="ml-1 text-primary">
                            {mark.dir}
                            {sort.length > 1 && <sup>{mark.priority}</sup>}
                          </span>
                        )}
                      </th>
                    );
                  })}
                  <th className="text-right">Действия</th>
                </tr>
              </thead>
              <tbody>
                {data?.items.map((m) => (
                  <tr key={m.id} className="hover">
                    {COLUMNS.map((c) => (
                      <td key={c.field} className="max-w-56 truncate">
                        {c.render(m)}
                      </td>
                    ))}
                    <td className="whitespace-nowrap text-right">
                      <button type="button" className="btn btn-ghost btn-xs" onClick={() => setModal({ kind: 'view', marine: m })}>
                        Открыть
                      </button>
                      <button type="button" className="btn btn-ghost btn-xs" onClick={() => setModal({ kind: 'edit', marine: m })}>
                        Изменить
                      </button>
                      <button type="button" className="btn btn-ghost btn-xs" onClick={() => setModal({ kind: 'patch', marine: m })}>
                        PATCH
                      </button>
                      <button type="button" className="btn btn-ghost btn-xs text-error" onClick={() => setModal({ kind: 'delete', marine: m })}>
                        Удалить
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {data && data.items.length === 0 && <EmptyState text="По заданным условиям ничего не найдено" />}
            {!data && !error && <EmptyState text="Загрузка…" />}
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

      <Modal open={modal?.kind === 'create'} title="Новый десантник" onClose={closeModal} wide>
        {modalError && (
          <div className="mb-3">
            <ErrorAlert error={modalError} />
          </div>
        )}
        {modal?.kind === 'create' && <MarineForm submitLabel="Создать" busy={busy} onSubmit={(input) => void create(input)} onCancel={closeModal} />}
      </Modal>

      <Modal open={modal?.kind === 'edit'} title={modal?.kind === 'edit' ? `Замена десантника №${modal.marine.id} (PUT)` : ''} onClose={closeModal} wide>
        {modalError && (
          <div className="mb-3">
            <ErrorAlert error={modalError} />
          </div>
        )}
        {modal?.kind === 'edit' && (
          <MarineForm initial={modal.marine} submitLabel="Заменить" busy={busy} onSubmit={(input) => void replace(modal.marine.id, input)} onCancel={closeModal} />
        )}
      </Modal>

      <Modal open={modal?.kind === 'patch'} title={modal?.kind === 'patch' ? `Частичное обновление №${modal.marine.id} (PATCH)` : ''} onClose={closeModal}>
        {modalError && (
          <div className="mb-3">
            <ErrorAlert error={modalError} />
          </div>
        )}
        {modal?.kind === 'patch' && (
          <>
            <TextArea
              label="Тело запроса (JSON)"
              value={patchBody}
              onChange={setPatchBody}
              rows={6}
              hint='Тело уходит как есть, поэтому различимы «поля нет» и «поле равно null»: {"achievements": null} очистит достижения, а {"name": null} даст 422.'
            />
            <div className="modal-action">
              <button type="button" className="btn" onClick={closeModal} disabled={busy}>
                Отмена
              </button>
              <button type="button" className="btn btn-primary" onClick={() => void patch(modal.marine.id)} disabled={busy}>
                {busy && <span className="loading loading-spinner loading-xs" />}
                Применить
              </button>
            </div>
          </>
        )}
      </Modal>

      <Modal open={modal?.kind === 'view'} title={modal?.kind === 'view' ? `Десантник №${modal.marine.id}` : ''} onClose={closeModal}>
        {modal?.kind === 'view' && (
          <>
            <p className="leading-relaxed">{describe(modal.marine)}</p>
            <div className="modal-action">
              <button type="button" className="btn" onClick={closeModal}>
                Закрыть
              </button>
            </div>
          </>
        )}
      </Modal>

      <Modal open={modal?.kind === 'delete'} title="Удалить десантника?" onClose={closeModal}>
        {modalError && (
          <div className="mb-3">
            <ErrorAlert error={modalError} />
          </div>
        )}
        {modal?.kind === 'delete' && (
          <>
            <p>
              Десантник №{modal.marine.id} «{modal.marine.name}» будет удалён без возможности восстановления.
            </p>
            <div className="modal-action">
              <button type="button" className="btn" onClick={closeModal} disabled={busy}>
                Отмена
              </button>
              <button type="button" className="btn btn-error" onClick={() => void remove(modal.marine.id)} disabled={busy}>
                {busy && <span className="loading loading-spinner loading-xs" />}
                Удалить
              </button>
            </div>
          </>
        )}
      </Modal>
    </>
  );
}
