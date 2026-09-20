import { useEffect, useState } from 'react';

/** Короткое уведомление об успешном действии, гаснет само. */
export function useToast(): [string | null, (message: string) => void] {
  const [toast, setToast] = useState<string | null>(null);

  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 3500);
    return () => clearTimeout(timer);
  }, [toast]);

  return [toast, setToast];
}
