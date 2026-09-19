import { useMemo, useRef, useState } from 'react';
import { SoaClient } from './api/client';
import { useEndpoints } from './config';
import { EndpointsScreen } from './screens/EndpointsScreen';
import { ExtrasScreen } from './screens/ExtrasScreen';
import { MarinesScreen } from './screens/MarinesScreen';
import { StarshipScreen } from './screens/StarshipScreen';

const TABS = ['Коллекция десантников', 'Сводные операции', 'Десантные корабли', 'Адреса сервисов'] as const;

/**
 * Клиентское приложение лабораторной работы №2.
 *
 * Закрывает все операции обоих сервисов: девять у SpaceMarine и две у Starship.
 * Данные показываются человеку в читаемом виде — таблицей, карточкой или фразой,
 * а ошибки сервисов разбираются и объясняются, включая перечень нарушенных ограничений.
 */
export default function App() {
  const [endpoints, setEndpoints] = useEndpoints();
  const [tab, setTab] = useState(0);

  // Клиент создаётся один раз и читает актуальные адреса через ref — так смена адресов
  // не пересоздаёт его и не сбрасывает состояние экранов.
  const endpointsRef = useRef(endpoints);
  endpointsRef.current = endpoints;
  const client = useMemo(() => new SoaClient(() => endpointsRef.current), []);

  return (
    <div className="app">
      <h1>СОА. Лабораторная работа №2 — клиентское приложение</h1>
      <nav className="tabs">
        {TABS.map((title, index) => (
          <button key={title} type="button" className={tab === index ? 'tab tab-on' : 'tab'} onClick={() => setTab(index)}>
            {title}
          </button>
        ))}
      </nav>
      {tab === 0 && <MarinesScreen client={client} />}
      {tab === 1 && <ExtrasScreen client={client} />}
      {tab === 2 && <StarshipScreen client={client} />}
      {tab === 3 && <EndpointsScreen value={endpoints} onChange={setEndpoints} />}
    </div>
  );
}
