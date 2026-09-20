import { ExtrasPage } from '@/pages/extras';
import { MarinesPage } from '@/pages/marines';
import { StarshipsPage } from '@/pages/starships';
import { Sidebar } from '@/widgets/sidebar';
import { useHashRoute } from './routing';

/**
 * Клиентское приложение лабораторной работы №2.
 *
 * Закрывает все операции обоих сервисов и показывает ответы в читаемом виде,
 * включая перечень нарушенных ограничений из ошибок.
 */
export function App() {
  const [page, navigate] = useHashRoute();

  return (
    <div className="flex min-h-screen bg-base-200">
      <Sidebar active={page} onNavigate={navigate} />
      <main className="min-w-0 flex-1 p-6">
        {page === 'marines' && <MarinesPage />}
        {page === 'extras' && <ExtrasPage />}
        {page === 'starships' && <StarshipsPage />}
      </main>
    </div>
  );
}
