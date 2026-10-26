import { useState, type ReactNode } from "react";

/** Carte d'information repliable (bouton « Masquer / Afficher les détails »). */
export function InfoCard({ title, children }: { title: string; children: ReactNode }) {
  const [replie, setReplie] = useState(false);
  return <article className={`card info-card ${replie ? "replie" : ""}`}>
    <div className="info-head"><h2>{title}</h2><button onClick={() => setReplie(!replie)} aria-expanded={!replie}>{replie ? "Afficher les détails" : "Masquer les détails"} <span>{replie ? "⌄" : "⌃"}</span></button></div>
    {children}
  </article>;
}

export function Info({ label, value }: { label: string; value: string | null | undefined }) {
  return <div className="info-item"><span>{label}</span><strong>{value || "—"}</strong></div>;
}
