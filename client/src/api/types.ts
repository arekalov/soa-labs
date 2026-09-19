/** Транспортные модели, повторяющие схемы спецификации из docs/*.yaml. */

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
  /** Остаётся строкой: клиенту нужно только показать её человеку. */
  creationDate: string;
  health: number;
  loyal: boolean;
  achievements: string | null;
  category: Category;
  chapter: ChapterDto | null;
}

/**
 * Тело создания и полной замены. Все поля необязательны намеренно: отсутствие
 * обязательного поля — это нарушение ограничений (422 с перечнем), которое
 * должен диагностировать сервис, а не клиент.
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

export interface SpaceMarinePageDto {
  items: SpaceMarineDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface IdGroupDto {
  id: number;
  count: number;
}

export interface CountResultDto {
  count: number;
}

export interface StarshipDto {
  id: number;
  name: string;
  marines: number[];
}

export interface UnloadResultDto {
  starshipId: number;
  spaceMarineId: number;
  message: string;
}

/** Схема `Error`. `details` заполняется при нарушении ограничений целостности (422). */
export interface ErrorDto {
  code: number;
  message: string;
  details?: string[] | null;
}
