/** Barres horizontales proportionnelles à la plus grande valeur ; la première est mise en évidence. */
export function HorizontalBars({ data, onSelect }: { data: readonly (readonly [string, number])[]; onSelect?: (index: number) => void }) {
  const maximum = Math.max(1, ...data.map(([, v]) => v));
  if (data.length === 0) return <p className="etat-chargement">Aucune donnée.</p>;
  return <div className="hbars">{data.map(([label, val], i) =>
    <div className={`hbar ${onSelect ? "cliquable" : ""}`} key={label} onClick={onSelect ? () => onSelect(i) : undefined}>
      <span title={label}>{label}</span><div><i style={{ width: `${(val / maximum) * 100}%` }} className={i === 0 ? "highlight" : ""} /></div><b>{val}</b>
    </div>)}</div>;
}
