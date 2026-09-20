import { S } from '@/shared/config';
import type { ApiResult, ErrorDto } from './types';

const JSON_HEADERS = { 'Content-Type': 'application/json', Accept: 'application/json' };

/** Единственное место, где живёт fetch: и разбор ответа, и разбор схемы `Error`. */
export async function request<T>(url: string, init?: RequestInit): Promise<ApiResult<T>> {
  let response: Response;
  try {
    response = await fetch(url, init);
  } catch {
    return { ok: false, error: { code: 0, message: S.errors.byCode[0] } };
  }

  if (!response.ok) return { ok: false, error: await parseError(response) };
  if (response.status === 204) return { ok: true, value: undefined as T };

  try {
    return { ok: true, value: (await response.json()) as T };
  } catch {
    return { ok: false, error: { code: response.status, message: S.errors.notJson } };
  }
}

export const get = <T>(url: string) => request<T>(url);

export const post = <T>(url: string, body?: unknown) =>
  request<T>(url, body === undefined ? { method: 'POST' } : { method: 'POST', headers: JSON_HEADERS, body: JSON.stringify(body) });

export const patch = <T>(url: string, body: unknown) =>
  request<T>(url, { method: 'PATCH', headers: JSON_HEADERS, body: JSON.stringify(body) });

export const remove = (url: string) => request<void>(url, { method: 'DELETE' });

/** Любой HTTP-ответ означает, что сервис на связи; исключение — что нет. */
export async function ping(url: string): Promise<boolean> {
  try {
    await fetch(url);
    return true;
  } catch {
    return false;
  }
}

/** Собирает query-строку, пропуская пустые значения. Повторяющиеся параметры сохраняют порядок. */
export function queryString(params: Record<string, string | number | undefined>, repeated: Record<string, string[]> = {}): string {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && String(value).trim() !== '') query.append(key, String(value));
  }
  for (const [key, values] of Object.entries(repeated)) {
    for (const value of values) query.append(key, value);
  }
  return query.toString();
}

async function parseError(response: Response): Promise<ErrorDto> {
  try {
    const body = (await response.json()) as Partial<ErrorDto>;
    if (typeof body.code === 'number' && typeof body.message === 'string') {
      return { code: body.code, message: body.message, details: body.details ?? null };
    }
  } catch {
    // тело не JSON
  }
  return { code: response.status, message: S.errors.unexpected(response.status) };
}
