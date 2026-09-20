import type { PageDto } from '@/shared/api';

export const CATEGORIES = ['SCOUT', 'SUPPRESSOR', 'TACTICAL', 'HELIX'] as const;
export type Category = (typeof CATEGORIES)[number];

/** Одиннадцать полей, по которым спецификация разрешает фильтровать и сортировать. */
export const SORTABLE_FIELDS = [
  'id',
  'name',
  'coordinatesX',
  'coordinatesY',
  'creationDate',
  'health',
  'loyal',
  'achievements',
  'category',
  'chapterName',
  'chapterParentLegion',
] as const;
export type SortableField = (typeof SORTABLE_FIELDS)[number];

export interface CoordinatesDto {
  x: number | null;
  y: number | null;
}

export interface ChapterDto {
  name: string | null;
  parentLegion: string | null;
}

export interface SpaceMarineDto {
  id: number;
  name: string;
  coordinates: CoordinatesDto;
  creationDate: string;
  health: number;
  loyal: boolean;
  achievements: string | null;
  category: Category;
  chapter: ChapterDto | null;
}

/**
 * Тело создания и изменения. Все поля необязательны намеренно: отсутствие обязательного
 * поля — это нарушение ограничений (422 с перечнем), которое диагностирует сервис.
 */
export interface SpaceMarineInputDto {
  name?: string | null;
  coordinates?: CoordinatesDto | null;
  health?: number | null;
  loyal?: boolean | null;
  achievements?: string | null;
  category?: string | null;
  chapter?: ChapterDto | null;
}

export type SpaceMarinePageDto = PageDto<SpaceMarineDto>;

export interface CountResultDto {
  count: number;
}
