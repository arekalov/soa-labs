import type {
  CountResultDto,
  ErrorDto,
  IdGroupDto,
  SpaceMarineDto,
  SpaceMarineInputDto,
  SpaceMarinePageDto,
  StarshipDto,
  UnloadResultDto,
} from './types';

/**
 * Результат обращения к API.
 *
 * Ошибка несёт разобранную схему `Error`, а не только код: задание требует
 * информировать пользователя о том, что именно не так, вплоть до перечня
 * нарушенных ограничений.
 */
export type ApiResult<T> = { ok: true; value: T } | { ok: false; error: ErrorDto };

export interface Endpoints {
  spaceMarine: string;
  starship: string;
}

/** По умолчанию — адреса SSH-туннеля: высокие порты helios снаружи закрыты. */
export const DEFAULT_ENDPOINTS: Endpoints = {
  spaceMarine: 'https://localhost:24443',
  starship: 'https://localhost:24543/starship',
};

const JSON_HEADERS = { 'Content-Type': 'application/json', Accept: 'application/json' };

export class SoaClient {
  constructor(private readonly endpoints: () => Endpoints) {}

  private get marines(): string {
    return this.endpoints().spaceMarine.replace(/\/+$/, '');
  }

  private get ships(): string {
    return this.endpoints().starship.replace(/\/+$/, '');
  }

  // ------------------------------------------------------------ SpaceMarine

  listMarines(
    filters: Record<string, string>,
    sort: string[],
    page: number,
    size: number,
  ): Promise<ApiResult<SpaceMarinePageDto>> {
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

  getMarine(id: string): Promise<ApiResult<SpaceMarineDto>> {
    return this.call(`${this.marines}/space-marines/${encodeURIComponent(id)}`);
  }

  createMarine(input: SpaceMarineInputDto): Promise<ApiResult<SpaceMarineDto>> {
    return this.call(`${this.marines}/space-marines`, {
      method: 'POST',
      headers: JSON_HEADERS,
      body: JSON.stringify(input),
    });
  }

  replaceMarine(id: string, input: SpaceMarineInputDto): Promise<ApiResult<SpaceMarineDto>> {
    return this.call(`${this.marines}/space-marines/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: JSON_HEADERS,
      body: JSON.stringify(input),
    });
  }

  /** Тело уходит сырой строкой: только так клиент может послать явный `null`. */
  patchMarine(id: string, rawJson: string): Promise<ApiResult<SpaceMarineDto>> {
    return this.call(`${this.marines}/space-marines/${encodeURIComponent(id)}`, {
      method: 'PATCH',
      headers: JSON_HEADERS,
      body: rawJson,
    });
  }

  deleteMarine(id: string): Promise<ApiResult<void>> {
    return this.call(`${this.marines}/space-marines/${encodeURIComponent(id)}`, { method: 'DELETE' });
  }

  minHealth(): Promise<ApiResult<SpaceMarineDto>> {
    return this.call(`${this.marines}/space-marines/health/min`);
  }

  groupsById(): Promise<ApiResult<IdGroupDto[]>> {
    return this.call(`${this.marines}/space-marines/groups/by-id`);
  }

  countByChapter(name: string, parentLegion: string): Promise<ApiResult<CountResultDto>> {
    const query = new URLSearchParams({ name });
    if (parentLegion.trim() !== '') query.append('parentLegion', parentLegion);
    return this.call(`${this.marines}/space-marines/count/by-chapter?${query}`);
  }

  // -------------------------------------------------------------- Starship

  createStarship(id: string, name: string): Promise<ApiResult<StarshipDto>> {
    return this.call(`${this.ships}/create/${encodeURIComponent(id)}/${encodeURIComponent(name)}`, {
      method: 'POST',
    });
  }

  unload(starshipId: string, spaceMarineId: string): Promise<ApiResult<UnloadResultDto>> {
    return this.call(
      `${this.ships}/${encodeURIComponent(starshipId)}/unload/${encodeURIComponent(spaceMarineId)}`,
      { method: 'POST' },
    );
  }

  // ----------------------------------------------------------- инфраструктура

  private async call<T>(url: string, init?: RequestInit): Promise<ApiResult<T>> {
    let response: Response;
    try {
      response = await fetch(url, init);
    } catch (e) {
      return { ok: false, error: transportError(e) };
    }

    if (response.ok) {
      // 204 у DELETE: тела нет, и читать его нельзя
      if (response.status === 204) return { ok: true, value: undefined as T };
      try {
        return { ok: true, value: (await response.json()) as T };
      } catch {
        return { ok: false, error: { code: response.status, message: 'Сервис вернул ответ не в формате JSON' } };
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
    // тело не JSON — например, страница контейнера
  }
  return { code: response.status, message: `Сервис вернул неожиданный ответ: ${response.status} ${response.statusText}` };
}

/**
 * До сервиса не дошли вовсе.
 *
 * Самая вероятная причина в этой лабораторной — браузер не доверяет самоподписанному
 * сертификату, поэтому подсказка говорит именно об этом, а не про абстрактную сеть.
 */
function transportError(e: unknown): ErrorDto {
  return {
    code: 0,
    message: 'Не удалось связаться с сервисом',
    details: [
      e instanceof Error ? e.message : String(e),
      'Проверьте, что сервис запущен, туннель поднят и сертификат принят браузером: ' +
        'откройте адрес сервиса в соседней вкладке и подтвердите исключение.',
    ],
  };
}
