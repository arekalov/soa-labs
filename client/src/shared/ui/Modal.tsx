import type { ReactNode } from 'react';
import { S } from '@/shared/config';

export function Modal({
  open,
  title,
  onClose,
  children,
  wide,
}: {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
  wide?: boolean;
}) {
  return (
    <dialog className={`modal ${open ? 'modal-open' : ''}`}>
      <div className={`modal-box ${wide ? 'max-w-4xl' : ''}`}>
        <h3 className="mb-3 text-lg font-bold">{title}</h3>
        {children}
      </div>
      <form method="dialog" className="modal-backdrop">
        <button type="button" onClick={onClose}>
          {S.common.close}
        </button>
      </form>
    </dialog>
  );
}
