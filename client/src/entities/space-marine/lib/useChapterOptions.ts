import { useEffect, useState } from 'react';
import { listMarines } from '../api/spaceMarineApi';

export interface ChapterOptions {
  chapters: string[];
  legions: string[];
}

const SAMPLE_SIZE = 100;

/**
 * Существующие ордены и легионы — для подсказок в полях ввода.
 * Отдельного эндпоинта нет, поэтому значения собираются из коллекции.
 */
export function useChapterOptions(version = 0): ChapterOptions {
  const [options, setOptions] = useState<ChapterOptions>({ chapters: [], legions: [] });

  useEffect(() => {
    let cancelled = false;
    void listMarines({}, ['chapterName'], 0, SAMPLE_SIZE).then((result) => {
      if (cancelled || !result.ok) return;
      const chapters = new Set<string>();
      const legions = new Set<string>();
      for (const marine of result.value.items) {
        if (marine.chapter?.name) chapters.add(marine.chapter.name);
        if (marine.chapter?.parentLegion) legions.add(marine.chapter.parentLegion);
      }
      setOptions({ chapters: [...chapters].sort(), legions: [...legions].sort() });
    });
    return () => {
      cancelled = true;
    };
  }, [version]);

  return options;
}
