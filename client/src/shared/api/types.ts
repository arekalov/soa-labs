/** Схема `Error`. `details` заполняется при нарушении ограничений целостности (422). */
export interface ErrorDto {
  code: number;
  message: string;
  details?: string[] | null;
}

/** Результат обращения к API: либо значение, либо разобранная схема `Error`. */
export type ApiResult<T> = { ok: true; value: T } | { ok: false; error: ErrorDto };

export interface PageDto<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
