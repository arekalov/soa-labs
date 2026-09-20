export interface SortMark {
  dir: string;
  priority: number;
  multi: boolean;
}

/** Клик по колонке: по возрастанию → по убыванию → без сортировки. Порядок кликов задаёт приоритет. */
export function toggleSortToken(current: string[], field: string): string[] {
  if (current.includes(field)) return [...current.filter((token) => token !== field), `-${field}`];
  if (current.includes(`-${field}`)) return current.filter((token) => token !== `-${field}`);
  return [...current, field];
}

export function sortMarkOf(sort: string[], field: string): SortMark | null {
  const index = sort.findIndex((token) => token === field || token === `-${field}`);
  if (index < 0) return null;
  return { dir: sort[index] === field ? '↑' : '↓', priority: index + 1, multi: sort.length > 1 };
}
