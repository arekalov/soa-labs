import { useCallback, useEffect, useState, type ReactNode } from 'react';
import type { SoaClient } from '../api/client';
import type { Theme } from '../theme';

export type PageKey = 'marines' | 'extras' | 'starships';

const NAV: { key: PageKey; title: string; caption: string }[] = [
  { key: 'marines', title: 'Десантники', caption: 'коллекция, фильтры, сортировка' },
  { key: 'extras', title: 'Сводные операции', caption: 'минимум, группы, подсчёт' },
  { key: 'starships', title: 'Корабли', caption: 'создание и высадка' },
];

type Status = 'unknown' | 'ok' | 'down';

function StatusDot({ status }: { status: Status }) {
  const cls = status === 'ok' ? 'bg-success' : status === 'down' ? 'bg-error' : 'bg-base-300';
  return <span className={`inline-block size-2.5 rounded-full ${cls}`} />;
}

interface LayoutProps {
  active: PageKey;
  onNavigate: (k: PageKey) => void;
  client: SoaClient;
  theme: Theme;
  onToggleTheme: () => void;
  children: ReactNode;
}

export function Layout({ active, onNavigate, client, theme, onToggleTheme, children }: LayoutProps) {
  const [sm, setSm] = useState<Status>('unknown');
  const [ss, setSs] = useState<Status>('unknown');
  const [checking, setChecking] = useState(false);

  // Вместо отдельного экрана с адресами — индикатор связи: сразу видно,
  // поднят ли туннель и приняты ли сертификаты.
  const check = useCallback(async () => {
    setChecking(true);
    const [a, b] = await Promise.all([client.pingSpaceMarine(), client.pingStarship()]);
    setSm(a ? 'ok' : 'down');
    setSs(b ? 'ok' : 'down');
    setChecking(false);
  }, [client]);

  useEffect(() => {
    void check();
  }, [check]);

  return (
    <div className="flex min-h-screen bg-base-200">
      <aside className="flex w-64 shrink-0 flex-col border-r border-base-300 bg-base-100">
        <div className="flex items-start justify-between gap-2 px-5 pt-5 pb-3">
          <div>
            <div className="text-lg font-bold leading-tight">СОА · Лабораторная №2</div>
            <div className="text-xs opacity-60">вариант 73126</div>
          </div>
          <label className="btn btn-ghost btn-sm btn-circle swap swap-rotate" title={theme === 'dark' ? 'Светлая тема' : 'Тёмная тема'}>
            <input type="checkbox" checked={theme === 'dark'} onChange={onToggleTheme} aria-label="Переключить тему" />
            {/* swap-off виден в светлой теме, swap-on — в тёмной */}
            <svg className="swap-off size-5 fill-current" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M5.64 17l-.71.71a1 1 0 001.41 1.41l.71-.71A1 1 0 005.64 17zM5 12a1 1 0 00-1-1H3a1 1 0 000 2h1a1 1 0 001-1zm7-7a1 1 0 001-1V3a1 1 0 00-2 0v1a1 1 0 001 1zM5.64 7.05a1 1 0 001.41-1.41l-.71-.71a1 1 0 00-1.41 1.41zm12 .29a1 1 0 00.7-.29l.71-.71a1 1 0 10-1.41-1.41l-.71.71a1 1 0 00.71 1.7zM21 11h-1a1 1 0 000 2h1a1 1 0 000-2zm-9 8a1 1 0 00-1 1v1a1 1 0 002 0v-1a1 1 0 00-1-1zm6.36-2A1 1 0 0017 18.36l.71.71a1 1 0 001.41-1.41zM12 6.5a5.5 5.5 0 105.5 5.5A5.51 5.51 0 0012 6.5z" />
            </svg>
            <svg className="swap-on size-5 fill-current" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M21.64 13a1 1 0 00-1.05-.14 8.05 8.05 0 01-3.37.73 8.15 8.15 0 01-8.14-8.1 8.59 8.59 0 01.25-2A1 1 0 008 2.36a10.14 10.14 0 1014 11.69 1 1 0 00-.36-1.05z" />
            </svg>
          </label>
        </div>

        <ul className="menu w-full grow px-3">
          {NAV.map((item) => (
            <li key={item.key}>
              <a className={active === item.key ? 'menu-active' : ''} onClick={() => onNavigate(item.key)}>
                <div>
                  <div>{item.title}</div>
                  <div className="text-xs font-normal opacity-60">{item.caption}</div>
                </div>
              </a>
            </li>
          ))}
        </ul>

        <div className="border-t border-base-300 px-5 py-4 text-xs">
          <div className="mb-2 flex items-center justify-between">
            <span className="font-semibold uppercase tracking-wide opacity-60">Сервисы</span>
            <button type="button" className="btn btn-ghost btn-xs" onClick={() => void check()} disabled={checking}>
              {checking ? <span className="loading loading-spinner loading-xs" /> : 'проверить'}
            </button>
          </div>
          <div className="flex items-center gap-2 py-0.5">
            <StatusDot status={sm} /> SpaceMarine
          </div>
          <div className="flex items-center gap-2 py-0.5">
            <StatusDot status={ss} /> Starship
          </div>
          {(sm === 'down' || ss === 'down') && (
            <p className="mt-2 leading-snug opacity-70">Нет связи: поднимите SSH-туннель и примите самоподписанные сертификаты, открыв адреса сервисов во вкладках.</p>
          )}
        </div>
      </aside>

      <main className="min-w-0 flex-1 p-6">{children}</main>
    </div>
  );
}
