// Graphique statique de la maquette : à remplacer par Recharts branché sur les statistiques (F10).
export function BarChart({ stacked = false }: { stacked?: boolean }) {
  const vals = [48, 57, 46, 69, 61, 78];
  return <div className="bar-chart"><div className="chart-y"><span>80k</span><span>60k</span><span>40k</span><span>20k</span><span>0</span></div><div className="bars">{vals.map((v, i) => <div className="bar-col" key={i}><div className="bar-slot">{stacked ? <><i className="bar-light" style={{ height: `${v * .45}%` }} /><i className="bar-main stacked" style={{ height: `${v * .55}%` }} /></> : <i className={`bar-main ${i === 5 ? "accent" : ""}`} style={{ height: `${v}%` }} />}</div><span>{["Mai", "Juin", "Juil.", "Août", "Sept.", "Oct."][i]}</span></div>)}</div></div>;
}
