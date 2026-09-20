import { useChapterOptions } from '@/entities/space-marine';
import { CountMarinesByChapter } from '@/features/count-marines-by-chapter';
import { CountMarinesByHealth } from '@/features/count-marines-by-health';
import { SearchMarinesByName } from '@/features/search-marines-by-name';
import { S } from '@/shared/config';
import { PageHeader } from '@/shared/ui';

/** Три дополнительные операции первого сервиса — те, что заданы вариантом. */
export function ExtrasPage() {
  const options = useChapterOptions();

  return (
    <>
      <PageHeader title={S.extras.title} subtitle={S.extras.subtitle} />
      <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <CountMarinesByChapter options={options} />
        <CountMarinesByHealth />
        <SearchMarinesByName />
      </div>
    </>
  );
}
