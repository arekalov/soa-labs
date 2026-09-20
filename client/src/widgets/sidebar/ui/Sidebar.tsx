import { useCallback, useEffect, useState } from 'react';
import { pingSpaceMarineService } from '@/entities/space-marine';
import { pingStarshipService } from '@/entities/starship';
import { ThemeToggle } from '@/features/theme-toggle';
import { S } from '@/shared/config';

export type PageKey = 'marines' | 'extras' | 'starships';

const NAV: { key: PageKey; title: string; caption: string }[] = [
  { key: 'marines', title: S.nav.marines, caption: S.nav.marinesCaption },
  { key: 'extras', title: S.nav.extras, caption: S.nav.extrasCaption },
  { key: 'starships', title: S.nav.ships, caption: S.nav.shipsCaption },
];

type Status = 'unknown' | 'ok' | 'down';

function StatusDot({ status }: { status: Status }) {
  const tone = status === 'ok' ? 'bg-success' : status === 'down' ? 'bg-error' : 'bg-base-300';
  return <span className={`inline-block size-2.5 rounded-full ${tone}`} />;
}

export function Sidebar({ active, onNavigate }: { active: PageKey; onNavigate: (key: PageKey) => void }) {
  const [spaceMarine, setSpaceMarine] = useState<Status>('unknown');
  const [starship, setStarship] = useState<Status>('unknown');
  const [checking, setChecking] = useState(false);

  const check = useCallback(async () => {
    setChecking(true);
    const [first, second] = await Promise.all([pingSpaceMarineService(), pingStarshipService()]);
    setSpaceMarine(first ? 'ok' : 'down');
    setStarship(second ? 'ok' : 'down');
    setChecking(false);
  }, []);

  useEffect(() => {
    void check();
  }, [check]);

  return (
    <aside className="flex w-64 shrink-0 flex-col border-r border-base-300 bg-base-100">
      <div className="flex items-start justify-between gap-2 px-5 pt-5 pb-3">
        <div>
          <div className="text-lg font-bold leading-tight">{S.app.title}</div>
          <div className="text-xs opacity-60">{S.app.variant}</div>
        </div>
        <ThemeToggle />
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
          <span className="font-semibold uppercase tracking-wide opacity-60">{S.app.services}</span>
          <button type="button" className="btn btn-ghost btn-xs" onClick={() => void check()} disabled={checking}>
            {checking ? <span className="loading loading-spinner loading-xs" /> : S.app.check}
          </button>
        </div>
        <div className="flex items-center gap-2 py-0.5">
          <StatusDot status={spaceMarine} /> {S.app.spaceMarine}
        </div>
        <div className="flex items-center gap-2 py-0.5">
          <StatusDot status={starship} /> {S.app.starship}
        </div>
      </div>
    </aside>
  );
}
