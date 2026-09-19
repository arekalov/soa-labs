import { useState } from 'react';
import type { SoaClient } from '../api/client';
import { CATEGORIES, SORTABLE_FIELDS, type ErrorDto, type SpaceMarineDto, type SpaceMarineInputDto, type SpaceMarinePageDto } from '../api/types';
import { Btn, Card, CheckField, Chip, DataTable, ErrorBanner, Field, Hint, Paragraph, Row, SelectField, SuccessBanner } from '../components/ui';

interface Props {
  client: SoaClient;
}

export function MarinesScreen({ client }: Props) {
  const [filters, setFilters] = useState<Record<string, string>>({});
  const [sort, setSort] = useState<string[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const [pageData, setPageData] = useState<SpaceMarinePageDto | null>(null);
  const [error, setError] = useState<ErrorDto | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  /** Аргументы явные: состояние React обновляется асинхронно, и читать его сразу после set нельзя. */
  async function load(f: Record<string, string>, s: string[], p: number, sz: number) {
    setNotice(null);
    const r = await client.listMarines(f, s, p, sz);
    if (r.ok) {
      setError(null);
      setPageData(r.value);
    } else {
      setError(r.error);
    }
  }

  const reload = () => load(filters, sort, page, size);

  const goTo = (p: number) => {
    setPage(p);
    void load(filters, sort, p, size);
  };

  /** Клик по полю: по возрастанию → по убыванию → без сортировки по этому полю. */
  const toggleSort = (field: string) =>
    setSort((current) => {
      const asc = field;
      const desc = `-${field}`;
      if (current.includes(asc)) return [...current.filter((t) => t !== asc), desc];
      if (current.includes(desc)) return current.filter((t) => t !== desc);
      return [...current, asc];
    });

  const reset = () => {
    setFilters({});
    setSort([]);
    setPage(0);
    void load({}, [], 0, size);
  };

  return (
    <>
      <Card title="Фильтрация — все 11 полей класса">
        <Hint>Пустые поля не отправляются. Фильтры комбинируются по «И», сравнение точное.</Hint>
        <Row>
          {SORTABLE_FIELDS.map((field) => (
            <Field key={field} label={field} value={filters[field] ?? ''} onChange={(v) => setFilters({ ...filters, [field]: v })} />
          ))}
        </Row>
        <Row>
          <Btn primary onClick={() => goTo(0)}>Найти</Btn>
          <Btn onClick={reset}>Сбросить</Btn>
        </Row>
      </Card>

      <Card title="Сортировка — несколько ступеней, порядок задаёт приоритет">
        <div>
          {SORTABLE_FIELDS.map((field) => {
            const mark = sort.includes(field) ? ' ↑' : sort.includes(`-${field}`) ? ' ↓' : '';
            return (
              <Chip key={field} active={mark !== ''} onClick={() => toggleSort(field)}>
                {field + mark}
              </Chip>
            );
          })}
        </div>
        <Hint>{sort.length === 0 ? 'Ступени не выбраны — сервис отдаст записи по возрастанию id.' : `Порядок: ${sort.join(' → ')}`}</Hint>
      </Card>

      <Card title="Постраничный вывод">
        <Row>
          <Field label="страница (с 0)" value={String(page)} onChange={(v) => setPage(Math.max(0, Number.parseInt(v, 10) || 0))} />
          <Field label="размер" value={String(size)} onChange={(v) => setSize(Math.max(1, Number.parseInt(v, 10) || 20))} />
          <Btn primary onClick={reload}>Обновить</Btn>
          <Btn onClick={() => goTo(Math.max(0, page - 1))}>←</Btn>
          <Btn onClick={() => goTo(page + 1)}>→</Btn>
        </Row>
      </Card>

      {error && <ErrorBanner error={error} />}
      {notice && <SuccessBanner>{notice}</SuccessBanner>}

      {pageData && (
        <Card title="Коллекция">
          <Paragraph>
            Показана страница {pageData.page + 1} из {Math.max(1, pageData.totalPages)}; всего элементов — {pageData.totalElements}, на странице —{' '}
            {pageData.items.length}.
          </Paragraph>
          {pageData.items.length === 0 ? (
            <Paragraph>По заданным условиям ничего не найдено.</Paragraph>
          ) : (
            <DataTable
              headers={['id', 'Имя', 'Координаты', 'Создан', 'Здоровье', 'Верен', 'Достижения', 'Категория', 'Орден']}
              rows={pageData.items.map((m) => [
                String(m.id),
                m.name,
                `(${m.coordinates.x ?? '—'}, ${m.coordinates.y ?? '—'})`,
                m.creationDate,
                String(m.health),
                m.loyal ? 'да' : 'нет',
                m.achievements ?? '—',
                m.category,
                m.chapter ? `${m.chapter.name ?? ''}${m.chapter.parentLegion ? ` / ${m.chapter.parentLegion}` : ''}` : '—',
              ])}
            />
          )}
        </Card>
      )}

      <MarineEditor
        client={client}
        onDone={(message) => {
          setNotice(message);
          setError(null);
          void reload();
        }}
        onError={(e) => {
          setError(e);
          setNotice(null);
        }}
      />

      <SingleMarineTools
        client={client}
        onNotice={(m) => {
          setNotice(m);
          setError(null);
        }}
        onError={(e) => {
          setError(e);
          setNotice(null);
        }}
        onChanged={() => void reload()}
      />
    </>
  );
}

/** Создание и полная замена — обе операции принимают одну и ту же схему `SpaceMarineInput`. */
function MarineEditor({ client, onDone, onError }: { client: SoaClient; onDone: (m: string) => void; onError: (e: ErrorDto) => void }) {
  const [name, setName] = useState('');
  const [x, setX] = useState('');
  const [y, setY] = useState('');
  const [health, setHealth] = useState('');
  const [loyal, setLoyal] = useState(true);
  const [achievements, setAchievements] = useState('');
  const [category, setCategory] = useState<string>('TACTICAL');
  const [chapterName, setChapterName] = useState('');
  const [chapterLegion, setChapterLegion] = useState('');
  const [replaceId, setReplaceId] = useState('');

  const numberOrNull = (raw: string): number | null => {
    const n = Number(raw);
    return raw.trim() === '' || Number.isNaN(n) ? null : n;
  };

  const buildInput = (): SpaceMarineInputDto => ({
    name: name.trim() === '' ? null : name,
    coordinates: { x: numberOrNull(x), y: numberOrNull(y) },
    health: numberOrNull(health),
    loyal,
    achievements: achievements.trim() === '' ? null : achievements,
    category: category === '' ? null : category,
    chapter:
      chapterName.trim() === '' && chapterLegion.trim() === ''
        ? null
        : { name: chapterName.trim() === '' ? null : chapterName, parentLegion: chapterLegion.trim() === '' ? null : chapterLegion },
  });

  const create = async () => {
    const r = await client.createMarine(buildInput());
    if (r.ok) onDone(`Десантник создан, присвоен идентификатор ${r.value.id}.`);
    else onError(r.error);
  };

  const replace = async () => {
    const r = await client.replaceMarine(replaceId, buildInput());
    if (r.ok) onDone(`Элемент ${r.value.id} полностью заменён.`);
    else onError(r.error);
  };

  return (
    <Card title="Добавление и полная замена элемента">
      <Row>
        <Field label="name" value={name} onChange={setName} />
        <Field label="coordinates.x" value={x} onChange={setX} />
        <Field label="coordinates.y (≤ 12)" value={y} onChange={setY} />
        <Field label="health (> 0)" value={health} onChange={setHealth} />
        <CheckField label="loyal" checked={loyal} onChange={setLoyal} />
        <SelectField label="category" value={category} options={CATEGORIES} onChange={setCategory} />
        <Field label="achievements" value={achievements} onChange={setAchievements} />
        <Field label="chapter.name" value={chapterName} onChange={setChapterName} />
        <Field label="chapter.parentLegion" value={chapterLegion} onChange={setChapterLegion} />
      </Row>
      <Row>
        <Btn primary onClick={() => void create()}>Создать</Btn>
        <Field label="id для замены" value={replaceId} onChange={setReplaceId} />
        <Btn onClick={() => void replace()}>Заменить (PUT)</Btn>
      </Row>
      <Hint>Незаполненные поля отправляются как отсутствующие — сервис ответит 422 и перечислит нарушения.</Hint>
    </Card>
  );
}

/** Получение по идентификатору, частичное обновление и удаление. */
function SingleMarineTools({
  client,
  onNotice,
  onError,
  onChanged,
}: {
  client: SoaClient;
  onNotice: (m: string) => void;
  onError: (e: ErrorDto) => void;
  onChanged: () => void;
}) {
  const [id, setId] = useState('');
  const [patchBody, setPatchBody] = useState('{"achievements": null}');
  const [found, setFound] = useState<SpaceMarineDto | null>(null);

  const get = async () => {
    const r = await client.getMarine(id);
    if (r.ok) {
      setFound(r.value);
      onNotice('Элемент найден.');
    } else {
      setFound(null);
      onError(r.error);
    }
  };

  const remove = async () => {
    const r = await client.deleteMarine(id);
    if (r.ok) {
      setFound(null);
      onNotice(`Элемент ${id} удалён.`);
      onChanged();
    } else {
      onError(r.error);
    }
  };

  const patch = async () => {
    const r = await client.patchMarine(id, patchBody);
    if (r.ok) {
      setFound(r.value);
      onNotice(`Элемент ${r.value.id} обновлён.`);
      onChanged();
    } else {
      onError(r.error);
    }
  };

  return (
    <Card title="Операции над одним элементом">
      <Row>
        <Field label="id" value={id} onChange={setId} />
        <Btn primary onClick={() => void get()}>Получить</Btn>
        <Btn onClick={() => void remove()}>Удалить</Btn>
      </Row>

      {found && (
        // Карточка одного объекта — абзац текста, третий вид представления из задания
        <Paragraph>
          Десантник №{found.id}: {found.name}. Категория — {found.category}, здоровье — {found.health},{' '}
          {found.loyal ? 'верен Империуму' : 'не верен Империуму'}. Координаты ({found.coordinates.x}, {found.coordinates.y}). Создан{' '}
          {found.creationDate}.{' '}
          {found.chapter ? `Орден — ${found.chapter.name}${found.chapter.parentLegion ? `, легион ${found.chapter.parentLegion}` : ''}. ` : 'Орден не указан. '}
          {found.achievements ? `Достижения: ${found.achievements}.` : 'Достижений нет.'}
        </Paragraph>
      )}

      <Hint>
        Частичное обновление (PATCH). Тело передаётся как есть, поэтому различимы «поле отсутствует» и «поле равно null»:{' '}
        {'{"achievements": null}'} очистит достижения, а {'{"name": null}'} даст 422.
      </Hint>
      <Row>
        <Field label="тело запроса" value={patchBody} onChange={setPatchBody} wide multiline />
      </Row>
      <Btn primary onClick={() => void patch()}>Применить PATCH</Btn>
    </Card>
  );
}
