import { S } from '@/shared/config';

export function ToggleField({ label, checked, onChange }: { label: string; checked: boolean; onChange: (value: boolean) => void }) {
  return (
    <fieldset className="fieldset">
      <legend className="fieldset-legend">{label}</legend>
      <label className="label cursor-pointer gap-2">
        <input
          type="checkbox"
          className="toggle toggle-sm toggle-primary"
          checked={checked}
          onChange={(event) => onChange(event.target.checked)}
        />
        <span className="text-sm">{checked ? S.common.yes : S.common.no}</span>
      </label>
    </fieldset>
  );
}
