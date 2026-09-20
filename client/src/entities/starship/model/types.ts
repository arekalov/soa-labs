import type { PageDto } from '@/shared/api';

export interface StarshipDto {
  id: number;
  name: string;
  marines: number[];
}

export type StarshipPageDto = PageDto<StarshipDto>;

export interface UnloadResultDto {
  starshipId: number;
  spaceMarineId: number;
  message: string;
}
