import { useState } from 'react';
import { DEFAULT_ENDPOINTS, type Endpoints } from './api/client';

const STORAGE_KEY = 'soa-lab2.endpoints';

/** Адреса сервисов с сохранением в localStorage — чтобы не вводить заново после перезагрузки. */
export function useEndpoints(): [Endpoints, (next: Endpoints) => void] {
  const [endpoints, setEndpoints] = useState<Endpoints>(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) return { ...DEFAULT_ENDPOINTS, ...(JSON.parse(raw) as Partial<Endpoints>) };
    } catch {
      // повреждённое значение — берём умолчания
    }
    return DEFAULT_ENDPOINTS;
  });

  const update = (next: Endpoints) => {
    setEndpoints(next);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  };

  return [endpoints, update];
}
