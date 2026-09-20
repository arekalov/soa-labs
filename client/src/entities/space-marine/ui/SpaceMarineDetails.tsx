import { S } from '@/shared/config';
import { formatDate } from '@/shared/lib';
import { DetailList } from '@/shared/ui';
import type { SpaceMarineDto } from '../model/types';

const F = S.marine.fields;

export function SpaceMarineDetails({ marine }: { marine: SpaceMarineDto }) {
  return (
    <DetailList
      items={[
        { label: F.id, value: marine.id },
        { label: F.name, value: marine.name },
        { label: F.category, value: marine.category },
        { label: F.health, value: marine.health },
        { label: F.loyal, value: marine.loyal ? S.common.yes : S.common.no },
        {
          label: F.coordinates,
          value: `(${marine.coordinates.x ?? S.common.empty}; ${marine.coordinates.y ?? S.common.empty})`,
        },
        { label: F.chapter, value: marine.chapter?.name ?? S.common.empty },
        { label: F.legion, value: marine.chapter?.parentLegion ?? S.common.empty },
        { label: F.achievements, value: marine.achievements ?? S.common.empty },
        { label: F.created, value: formatDate(marine.creationDate) },
      ]}
    />
  );
}
