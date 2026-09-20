import type { Endpoints } from './endpoints';
import { S } from '../strings';
import type {
  CountResultDto,
  ErrorDto,
  SpaceMarineDto,
  SpaceMarineInputDto,
  SpaceMarinePageDto,
  StarshipDto,
  StarshipPageDto,
  UnloadResultDto,
} from './types';

/**
 * Результат обращения к API. Ошибка несёт разобранную схему `Error` целиком:
 * пользователю показывается и текст, и перечень нарушенных ограничений.
 */
export type ApiResult<T> = { ok: true; value: T } | { ok: false; error: ErrorDto };

const JSON_HEADERS = { 'Content-Type': 'application/json', Accept: 'application/json' };

export class SoaClient {
  constructor(private readonly endpoints: Endpoints) {}

  private get marines(): string {
    return this.endpoints.spaceMarine.replace(/\/+$/, '');
  }

  private get ships(): string {
    return this.endpoints.starship.replace(/\/+$/, '');
  }

  // ------------------------------------------------------------ SpaceMarine

  listMarines(filters: Record<string, string>, sort: string[], page: number, size: number): Promise<ApiResult<SpaceMarinePageDto>> {
    const query = new URLSearchParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value.trim() !== '') query.append(key, value);
    }
    // Повторяющийся параметр; порядок задаёт приоритет ступеней сортировки.
    for (const token of sort) query.append('sort', token);
    query.append('page', String(page));
    query.append('size', String(size));
    return this.call(`${this.marines}/space-marines?${query}`);
  }

  getMarine(id: number): Promise<ApiResult<SpaceMarineDto>> {
    return this.call(`${this.marines}/space-marines/${id}`);
  }

  createMarine(input: SpaceMarineInputDto): Promise<ApiResult<SpaceMarineDto>> {
    return this.call(`${this.marines}/space-marines`, { method: 'POST', headers: JSON_HEADERS, body: JSON.stringify(input) });
  }

  /** Явные `null` в патче сохраняются: JSON.stringify отбрасывает только `undefined`. */
  patchMarine(id: number, patch: SpaceMarineInputDto): Promise<ApiResult<SpaceMarineDto>> {
    return this.call(`${this.marines}/space-marines/${id}`, { method: 'PATCH', headers: JSON_HEADERS, body: JSON.stringify(patch) });
  }

  deleteMarine(id: number): Promise<ApiResult<void>> {
    return this.call(`${this.marines}/space-marines/${id}`, { method: 'DELETE' });
  }

  countByChapter(name: string, parentLegion: string): Promise<ApiResult<CountResultDto>> {
    const query = new URLSearchParams({ name });
    if (parentLegion.trim() !== '') query.append('parentLegion', parentLegion);
    return this.call(`${this.marines}/space-marines/count/by-chapter?${query}`);
  }

  countByHealthGreaterThan(health: string): Promise<ApiResult<CountResultDto>> {
    return this.call(`${this.marines}/space-marines/count/by-health-greater-than?${new URLSearchParams({ health })}`);
  }

  findByNamePrefix(prefix: string, page: number, size: number): Promise<ApiResult<SpaceMarinePageDto>> {
    const query = new URLSearchParams({ prefix, page: String(page), size: String(size) });
    return this.call(`${this.marines}/space-marines/search/by-name-prefix?${query}`);
  }

  // -------------------------------------------------------------- Starship

  listStarships(filters: Record<string, string>, sort: string[], page: number, size: number): Promise<ApiResult<StarshipPageDto>> {
    const query = new URLSearchParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value.trim() !== '') query.append(key, value);
    }
    for (const token of sort) query.append('sort', token);
    query.append('page', String(page));
    query.append('size', String(size));
    return this.call(`${this.ships}/?${query}`);
  }

  getStarship(id: number): Promise<ApiResult<StarshipDto>> {
    return this.call(`${this.ships}/${id}`);
  }

  createStarship(name: string): Promise<ApiResult<StarshipDto>> {
    return this.call(`${this.ships}/`, { method: 'POST', headers: JSON_HEADERS, body: JSON.stringify({ name }) });
  }

  renameStarship(id: number, name: string): Promise<ApiResult<StarshipDto>> {
    return this.call(`${this.ships}/${id}`, { method: 'PATCH', headers: JSON_HEADERS, body: JSON.stringify({ name }) });
  }

  deleteStarship(id: number): Promise<ApiResult<void>> {
    return this.call(`${this.ships}/${id}`, { method: 'DELETE' });
  }

  boardMarine(starshipId: number, spaceMarineId: number): Promise<ApiResult<StarshipDto>> {
    return this.call(`${this.ships}/${starshipId}/board/${spaceMarineId}`, { method: 'POST' });
  }

  unloadMarine(starshipId: number, spaceMarineId: number): Promise<ApiResult<UnloadResultDto>> {
    return this.call(`${this.ships}/${starshipId}/unload/${spaceMarineId}`, { method: 'POST' });
  }

  // ----------------------------------------------------------- доступность

  /** Любой HTTP-ответ означает, что сервис на связи; исключение — что нет. */
  async pingSpaceMarine(): Promise<boolean> {
    return this.ping(`${this.marines}/space-marines?size=1`);
  }

  async pingStarship(): Promise<boolean> {
    return this.ping(`${this.ships}/?size=1`);
  }

  private async ping(url: string): Promise<boolean> {
    try {
      await fetch(url);
      return true;
    } catch {
      return false;
    }
  }

  // ----------------------------------------------------------- инфраструктура

  private async call<T>(url: string, init?: RequestInit): Promise<ApiResult<T>> {
    let response: Response;
    try {
      response = await fetch(url, init);
    } catch {
      return { ok: false, error: { code: 0, message: S.errors.byCode[0] } };
    }

    if (response.ok) {
      if (response.status === 204) return { ok: true, value: undefined as T };
      try {
        return { ok: true, value: (await response.json()) as T };
      } catch {
        return { ok: false, error: { code: response.status, message: S.errors.notJson } };
      }
    }

    return { ok: false, error: await parseError(response) };
  }
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
