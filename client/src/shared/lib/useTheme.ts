import { useEffect, useState } from 'react';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'soa-lab2.theme';

function resolveInitialTheme(): Theme {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'light' || stored === 'dark') return stored;
  } catch {
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export function useTheme(): [Theme, () => void] {
  const [theme, setTheme] = useState<Theme>(resolveInitialTheme);

  useEffect(() => {
    // DaisyUI переключает палитру по атрибуту data-theme на <html>
    document.documentElement.dataset.theme = theme;
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch {
    }
  }, [theme]);

  return [theme, () => setTheme((current) => (current === 'dark' ? 'light' : 'dark'))];
}
