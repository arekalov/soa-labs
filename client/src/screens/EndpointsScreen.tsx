import type { Endpoints } from '../api/client';
import { Card, Field, Hint } from '../components/ui';

export function EndpointsScreen({ value, onChange }: { value: Endpoints; onChange: (next: Endpoints) => void }) {
  return (
    <Card title="Адреса сервисов">
      <Hint>
        Высокие порты helios закрыты снаружи, поэтому на защите сервисы доступны через SSH-туннель:
        <br />
        <code>ssh -N -L 24443:127.0.0.1:24443 -L 24543:127.0.0.1:24543 ifmo</code>
        <br />
        При этом адреса остаются localhost — именно они подставлены по умолчанию. Значения запоминаются в браузере.
      </Hint>
      <Field label="SpaceMarine Service" value={value.spaceMarine} wide onChange={(v) => onChange({ ...value, spaceMarine: v })} />
      <Field label="Starship Service" value={value.starship} wide onChange={(v) => onChange({ ...value, starship: v })} />
      <Hint>
        Сертификаты самоподписанные. Перед первым запросом откройте каждый адрес в отдельной вкладке и подтвердите исключение безопасности, иначе
        браузер молча отклонит обращения.
      </Hint>
    </Card>
  );
}
