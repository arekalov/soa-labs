import { useEffect, useState } from 'react';
import type { SoaClient } from '../api/client';

export interface ChapterOptions {
  chapters: string[];
  legions: string[];
}

/**
 * Существующие ордены и легионы — для подсказок в полях ввода.
 * Отдельного эндпоинта нет, поэтому значения собираются из коллекции.
 */
export function useChapterOptions(client: SoaClient, version = 0): ChapterOptions {
  const [options, setOptions] = useState<ChapterOptions>({ chapters: [], legions: [] });

  useEffect(() => {
    let cancelled = false;
    void client.listMarines({}, ['chapterName'], 0, 100).then((r) => {
      if (cancelled || !r.ok) return;
      const chapters = new Set<string>();
      const legions = new Set<string>();
      for (const m of r.value.items) {
        if (m.chapter?.name) chapters.add(m.chapter.name);
        if (m.chapter?.parentLegion) legions.add(m.chapter.parentLegion);
      }
      setOptions({ chapters: [...chapters].sort(), legions: [...legions].sort() });
    });
    return () => {
      cancelled = true;
    };
  }, [client, version]);

  return options;
}
