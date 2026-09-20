import { useState } from 'react';
import { countMarinesByChapter } from '@/entities/space-marine';
import type { ChapterOptions } from '@/entities/space-marine';
import type { ErrorDto } from '@/shared/api';
import { S } from '@/shared/config';
import { ErrorAlert, Panel, TextField } from '@/shared/ui';

/** Количество десантников, у которых орден равен заданному. */
export function CountMarinesByChapter({ options }: { options: ChapterOptions }) {
  const [chapter, setChapter] = useState('');
  const [legion, setLegion] = useState('');
  const [count, setCount] = useState<number | null>(null);
  const [error, setError] = useState<ErrorDto | null>(null);

  const submit = async () => {
    const result = await countMarinesByChapter(chapter, legion);
    setError(result.ok ? null : result.error);
    setCount(result.ok ? result.value.count : null);
  };

  return (
    <Panel title={S.extras.byChapter}>
      <form
        className="grid grid-cols-2 gap-x-4"
        onSubmit={(event) => {
          event.preventDefault();
          void submit();
        }}
      >
        <TextField label={S.marine.fields.chapter} value={chapter} onChange={setChapter} options={options.chapters} />
        <TextField
          label={S.marine.fields.legion}
          value={legion}
          onChange={setLegion}
          options={options.legions}
          hint={S.extras.legionHint}
        />
        <div className="col-span-2 flex items-center gap-4">
          <button type="submit" className="btn btn-sm btn-primary">
            {S.extras.count}
          </button>
          {count !== null && (
            <div className="stat px-0 py-0">
              <div className="stat-title text-xs">{S.extras.marinesCount}</div>
              <div className="stat-value text-2xl">{count}</div>
            </div>
          )}
        </div>
      </form>
      {error && <ErrorAlert error={error} onClose={() => setError(null)} />}
    </Panel>
  );
}
