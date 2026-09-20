import { get, patch, ping, post, queryString, remove, SPACE_MARINE_API } from '@/shared/api';
import type { ApiResult } from '@/shared/api';
import type { CountResultDto, SpaceMarineDto, SpaceMarineInputDto, SpaceMarinePageDto } from '../model/types';

const BASE = `${SPACE_MARINE_API}/space-marines`;

export function listMarines(
  filters: Record<string, string>,
  sort: string[],
  page: number,
  size: number,
): Promise<ApiResult<SpaceMarinePageDto>> {
  // sort — повторяющийся параметр, порядок задаёт приоритет ступеней.
  return get(`${BASE}?${queryString({ ...filters, page, size }, { sort })}`);
}

export const getMarine = (id: number): Promise<ApiResult<SpaceMarineDto>> => get(`${BASE}/${id}`);

export const createMarine = (input: SpaceMarineInputDto): Promise<ApiResult<SpaceMarineDto>> => post(BASE, input);

/** Явные `null` в патче сохраняются: JSON.stringify отбрасывает только `undefined`. */
export const patchMarine = (id: number, body: SpaceMarineInputDto): Promise<ApiResult<SpaceMarineDto>> =>
  patch(`${BASE}/${id}`, body);

export const deleteMarine = (id: number): Promise<ApiResult<void>> => remove(`${BASE}/${id}`);

export const countMarinesByChapter = (name: string, parentLegion: string): Promise<ApiResult<CountResultDto>> =>
  get(`${BASE}/count/by-chapter?${queryString({ name, parentLegion })}`);

export const countMarinesByHealthGreaterThan = (health: string): Promise<ApiResult<CountResultDto>> =>
  get(`${BASE}/count/by-health-greater-than?${queryString({ health })}`);

export const findMarinesByNamePrefix = (
  prefix: string,
  page: number,
  size: number,
): Promise<ApiResult<SpaceMarinePageDto>> => get(`${BASE}/search/by-name-prefix?${queryString({ prefix, page, size })}`);

export const pingSpaceMarineService = (): Promise<boolean> => ping(`${BASE}?size=1`);
