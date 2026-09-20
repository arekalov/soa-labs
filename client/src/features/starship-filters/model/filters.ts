/** Псевдоним, а не интерфейс: так тип подходит под `Record<string, string>` в запросе. */
export type StarshipFilters = {
  id: string;
  name: string;
};

export const EMPTY_STARSHIP_FILTERS: StarshipFilters = { id: '', name: '' };
