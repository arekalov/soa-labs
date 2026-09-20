import { useEffect, useState } from 'react';
import type { PageKey } from '@/widgets/sidebar';

const PAGES: PageKey[] = ['marines', 'extras', 'starships'];

function pageFromHash(): PageKey {
  const key = window.location.hash.replace(/^#\/?/, '');
  return (PAGES as string[]).includes(key) ? (key as PageKey) : 'marines';
}

/** Раздел живёт в хеше адреса, чтобы переживать обновление страницы. */
export function useHashRoute(): [PageKey, (key: PageKey) => void] {
  const [page, setPage] = useState<PageKey>(pageFromHash);

  useEffect(() => {
    const sync = () => setPage(pageFromHash());
    window.addEventListener('hashchange', sync);
    return () => window.removeEventListener('hashchange', sync);
  }, []);

  return [
    page,
    (key: PageKey) => {
      window.location.hash = `/${key}`;
    },
  ];
}
