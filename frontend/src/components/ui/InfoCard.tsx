import type { ReactNode } from "react";

export function InfoCard({ title, children }: { title: string; children: ReactNode }) {
  return <article className="card info-card"><div className="info-head"><h2>{title}</h2><button>Afficher les détails <span>⌄</span></button></div>{children}</article>;
}

export function Info({ label, value }: { label: string; value: string }) {
  return <div className="info-item"><span>{label}</span><strong>{value}</strong></div>;
}
