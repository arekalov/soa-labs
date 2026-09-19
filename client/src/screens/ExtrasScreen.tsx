import { useState } from 'react';
import type { SoaClient } from '../api/client';
import type { ErrorDto, IdGroupDto, SpaceMarineDto } from '../api/types';
import { Btn, Card, DataTable, ErrorBanner, Field, Hint, Paragraph, Row } from '../components/ui';

/**
 * Три дополнительные операции первого сервиса.
 *
 * Каждая показана в естественном для неё виде: фраза для одного объекта,
 * таблица для группировки, фраза для числа.
 */
export function ExtrasScreen({ client }: { client: SoaClient }) {
  const [error, setError] = useState<ErrorDto | null>(null);
  const [minHealth, setMinHealth] = useState<SpaceMarineDto | null>(null);
  const [groups, setGroups] = useState<IdGroupDto[] | null>(null);
  const [chapterName, setChapterName] = useState('Ultramarines');
  const [parentLegion, setParentLegion] = useState('');
  const [countText, setCountText] = useState<string | null>(null);

  const loadMin = async () => {
    const r = await client.minHealth();
    if (r.ok) {
      setMinHealth(r.value);
      setError(null);
    } else {
      setMinHealth(null);
      setError(r.error);
    }
  };

  const loadGroups = async () => {
    const r = await client.groupsById();
    if (r.ok) {
      setGroups(r.value);
      setError(null);
    } else {
      setGroups(null);
      setError(r.error);
    }
  };

  const count = async () => {
    const r = await client.countByChapter(chapterName, parentLegion);
    if (r.ok) {
      setError(null);
      const legion = parentLegion.trim() === '' ? 'любого легиона' : `легиона «${parentLegion}»`;
      setCountText(`В ордене «${chapterName}» (${legion}) числится десантников: ${r.value.count}.`);
    } else {
      setCountText(null);
      setError(r.error);
    }
  };

  return (
    <>
      {error && <ErrorBanner error={error} />}

      <Card title="Десантник с минимальным здоровьем">
        <Btn primary onClick={() => void loadMin()}>Запросить</Btn>
        {minHealth && (
          <Paragraph>
            Наименьшее здоровье — {minHealth.health} — у десантника №{minHealth.id} по имени {minHealth.name}, категория {minHealth.category}
            {minHealth.chapter?.name ? `, орден ${minHealth.chapter.name}` : ''}.
          </Paragraph>
        )}
        <Hint>Если коллекция пуста, сервис отвечает 404 — это предусмотрено спецификацией.</Hint>
      </Card>

      <Card title="Группировка по идентификатору">
        <Btn primary onClick={() => void loadGroups()}>Сгруппировать</Btn>
        {groups &&
          (groups.length === 0 ? (
            <Paragraph>Коллекция пуста — групп нет.</Paragraph>
          ) : (
            <>
              <Paragraph>Групп: {groups.length}.</Paragraph>
              <DataTable headers={['Значение id', 'Элементов в группе']} rows={groups.map((g) => [String(g.id), String(g.count)])} />
            </>
          ))}
        <Hint>Идентификатор уникален, поэтому в каждой группе ожидаемо ровно один элемент.</Hint>
      </Card>

      <Card title="Количество десантников заданного ордена">
        <Row>
          <Field label="name (обязательно)" value={chapterName} onChange={setChapterName} />
          <Field label="parentLegion" value={parentLegion} onChange={setParentLegion} />
          <Btn primary onClick={() => void count()}>Посчитать</Btn>
        </Row>
        {countText && <Paragraph>{countText}</Paragraph>}
        <Hint>Без указания легиона считаются десантники ордена независимо от легиона.</Hint>
      </Card>
    </>
  );
}
