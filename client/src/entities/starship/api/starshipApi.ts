import { get, patch, ping, post, queryString, remove, STARSHIP_API } from '@/shared/api';
import type { ApiResult } from '@/shared/api';
import type { StarshipDto, StarshipPageDto, UnloadResultDto } from '../model/types';

// Завершающий слэш обязателен: иначе Tomcat отвечает на корень контекста перенаправлением 302.
const BASE = `${STARSHIP_API}/`;

export function listStarships(
  filters: Record<string, string>,
  sort: string[],
  page: number,
  size: number,
): Promise<ApiResult<StarshipPageDto>> {
  return get(`${BASE}?${queryString({ ...filters, page, size }, { sort })}`);
}

export const getStarship = (id: number): Promise<ApiResult<StarshipDto>> => get(`${STARSHIP_API}/${id}`);

export const createStarship = (name: string): Promise<ApiResult<StarshipDto>> => post(BASE, { name });

export const renameStarship = (id: number, name: string): Promise<ApiResult<StarshipDto>> =>
  patch(`${STARSHIP_API}/${id}`, { name });

export const deleteStarship = (id: number): Promise<ApiResult<void>> => remove(`${STARSHIP_API}/${id}`);

export const boardMarine = (starshipId: number, spaceMarineId: number): Promise<ApiResult<StarshipDto>> =>
  post(`${STARSHIP_API}/${starshipId}/board/${spaceMarineId}`);

export const unloadMarine = (starshipId: number, spaceMarineId: number): Promise<ApiResult<UnloadResultDto>> =>
  post(`${STARSHIP_API}/${starshipId}/unload/${spaceMarineId}`);

export const pingStarshipService = (): Promise<boolean> => ping(`${BASE}?size=1`);
