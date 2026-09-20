import { useCallback, useEffect, useState, type ReactNode } from 'react';
import type { SoaClient } from '../api/client';

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

export function Layout({ active, onNavigate, client, children }: { active: PageKey; onNavigate: (k: PageKey) => void; client: SoaClient; children: ReactNode }) {
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
        <div className="px-5 pt-5 pb-3">
          <div className="text-lg font-bold leading-tight">СОА · Лабораторная №2</div>
          <div className="text-xs opacity-60">вариант 73126</div>
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
