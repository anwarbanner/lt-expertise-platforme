/** Barres horizontales : libellé, valeur, et la première barre mise en évidence. */
export function HorizontalBars({ data }: { data: readonly (readonly [string, number])[] }) {
  return <div className="hbars">{data.map(([label, val], i) => <div className="hbar" key={label}><span>{label}</span><div><i style={{ width: `${val}%` }} className={i === 0 ? "highlight" : ""} /></div><b>{val}</b></div>)}</div>;
}
