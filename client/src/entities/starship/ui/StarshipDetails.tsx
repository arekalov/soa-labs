import { S } from '@/shared/config';
import { DetailList } from '@/shared/ui';
import type { StarshipDto } from '../model/types';

const F = S.ship.fields;

export function StarshipDetails({ starship }: { starship: StarshipDto }) {
  return (
    <DetailList
      items={[
        { label: F.id, value: starship.id },
        { label: F.name, value: starship.name },
      ]}
    />
  );
}
