import { useMemo, useState } from 'react';
import { SoaClient } from './api/client';
import { ENDPOINTS } from './api/endpoints';
import { Layout, type PageKey } from './components/Layout';
import { ExtrasPage } from './pages/ExtrasPage';
import { MarinesPage } from './pages/MarinesPage';
import { StarshipsPage } from './pages/StarshipsPage';
import { useTheme } from './theme';

/**
 * Клиентское приложение лабораторной работы №2.
 *
 * Закрывает все операции обоих сервисов: девять у SpaceMarine и две у Starship.
 * Данные показываются человеку в читаемом виде — таблицей, карточкой или фразой,
 * а ошибки сервисов разбираются и объясняются, включая перечень нарушенных ограничений.
 */
export default function App() {
  const client = useMemo(() => new SoaClient(ENDPOINTS), []);
  const [page, setPage] = useState<PageKey>('marines');
  const [theme, toggleTheme] = useTheme();

  return (
    <Layout active={page} onNavigate={setPage} client={client} theme={theme} onToggleTheme={toggleTheme}>
      {page === 'marines' && <MarinesPage client={client} />}
      {page === 'extras' && <ExtrasPage client={client} />}
      {page === 'starships' && <StarshipsPage client={client} />}
    </Layout>
  );
}
