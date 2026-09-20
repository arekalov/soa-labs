import { SORTABLE_FIELDS } from '@/entities/space-marine';
import type { SortableField } from '@/entities/space-marine';

export type MarineFilters = Record<SortableField, string>;

export const EMPTY_MARINE_FILTERS = Object.fromEntries(SORTABLE_FIELDS.map((field) => [field, ''])) as MarineFilters;

export const countActive = (filters: Record<string, string>): number =>
  Object.values(filters).filter((value) => value.trim() !== '').length;
