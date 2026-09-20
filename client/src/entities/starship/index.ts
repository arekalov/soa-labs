export {
  boardMarine,
  createStarship,
  deleteStarship,
  getStarship,
  listStarships,
  pingStarshipService,
  renameStarship,
  unloadMarine,
} from './api/starshipApi';
export type { StarshipDto, StarshipPageDto, UnloadResultDto } from './model/types';
export { StarshipDetails } from './ui/StarshipDetails';
