import type { SpaceMarineDto, SpaceMarineInputDto } from '@/entities/space-marine';

const INPUT_KEYS = ['name', 'coordinates', 'health', 'loyal', 'achievements', 'category', 'chapter'] as const;

/** Полный набор полей десантника в виде тела запроса. */
export function marineToInput(marine: SpaceMarineDto): SpaceMarineInputDto {
  return {
    name: marine.name,
    coordinates: { x: marine.coordinates.x, y: marine.coordinates.y },
    health: marine.health,
    loyal: marine.loyal,
    achievements: marine.achievements,
    category: marine.category,
    chapter: marine.chapter ? { name: marine.chapter.name, parentLegion: marine.chapter.parentLegion } : null,
  };
}

/** Только изменившиеся поля — тело для PATCH. Пустой объект означает, что менять нечего. */
export function diffInput(before: SpaceMarineInputDto, after: SpaceMarineInputDto): SpaceMarineInputDto {
  const patch: Record<string, unknown> = {};
  for (const key of INPUT_KEYS) {
    if (JSON.stringify(before[key] ?? null) !== JSON.stringify(after[key] ?? null)) patch[key] = after[key] ?? null;
  }
  return patch as SpaceMarineInputDto;
}
