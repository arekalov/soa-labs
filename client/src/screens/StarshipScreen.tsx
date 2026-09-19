import { useState } from 'react';
import type { SoaClient } from '../api/client';
import type { ErrorDto, StarshipDto } from '../api/types';
import { Btn, Card, ErrorBanner, Field, Hint, Paragraph, Row, SuccessBanner } from '../components/ui';

/**
 * Обе операции второго сервиса.
 *
 * Высадка примечательна тем, что второй сервис при её выполнении сам обращается
 * к первому по HTTPS. Поэтому здесь возможен ответ 503 — он означает, что недоступен
 * именно первый сервис, а не тот, к которому мы обратились.
 */
export function StarshipScreen({ client }: { client: SoaClient }) {
  const [error, setError] = useState<ErrorDto | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [shipId, setShipId] = useState('1');
  const [shipName, setShipName] = useState("Macragge's Honour");
  const [created, setCreated] = useState<StarshipDto | null>(null);

  const [unloadShip, setUnloadShip] = useState('1');
  const [unloadMarine, setUnloadMarine] = useState('1');

  const create = async () => {
    const r = await client.createStarship(shipId, shipName);
    if (r.ok) {
      setCreated(r.value);
      setError(null);
      setNotice(`Корабль «${r.value.name}» создан с идентификатором ${r.value.id} и сохранён в базе данных сервиса.`);
    } else {
      setCreated(null);
      setNotice(null);
      setError(r.error);
    }
  };

  const unload = async () => {
    const r = await client.unload(unloadShip, unloadMarine);
    if (r.ok) {
      setError(null);
      setNotice(r.value.message);
    } else {
      setNotice(null);
      setError(r.error);
    }
  };

  return (
    <>
      {error && <ErrorBanner error={error} />}
      {notice && <SuccessBanner>{notice}</SuccessBanner>}

      <Card title="Создание десантного корабля">
        <Row>
          <Field label="id (> 0)" value={shipId} onChange={setShipId} />
          <Field label="название" value={shipName} onChange={setShipName} />
          <Btn primary onClick={() => void create()}>Создать</Btn>
        </Row>
        {created && (
          <Paragraph>
            Корабль №{created.id} — «{created.name}». {created.marines.length === 0 ? 'На борту сейчас никого нет.' : `На борту десантники: ${created.marines.join(', ')}.`}
          </Paragraph>
        )}
        <Hint>Повторное создание корабля с тем же идентификатором даёт 409 — так требует спецификация.</Hint>
      </Card>

      <Card title="Высадка десантника с корабля">
        <Row>
          <Field label="id корабля" value={unloadShip} onChange={setUnloadShip} />
          <Field label="id десантника" value={unloadMarine} onChange={setUnloadMarine} />
          <Btn primary onClick={() => void unload()}>Высадить</Btn>
        </Row>
        <Hint>
          Ответ 404 приходит в трёх случаях: корабля нет, десантника нет в первом сервисе либо он не на этом корабле — сообщение поясняет, какой
          именно. Ответ 503 означает, что первый сервис недоступен.
        </Hint>
        <Hint>Операции погрузки спецификация не предусматривает, поэтому для показа успешной высадки состав экипажа задаётся заранее на стороне сервиса.</Hint>
      </Card>
    </>
  );
}
