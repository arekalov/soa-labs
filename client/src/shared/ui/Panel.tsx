import type { ReactNode } from 'react';

export function Panel({ title, children, className = '' }: { title?: string; children: ReactNode; className?: string }) {
  return (
    <section className={`card bg-base-100 shadow-sm ${className}`}>
      <div className="card-body gap-3">
        {title && <h2 className="card-title text-base">{title}</h2>}
        {children}
      </div>
    </section>
  );
}
