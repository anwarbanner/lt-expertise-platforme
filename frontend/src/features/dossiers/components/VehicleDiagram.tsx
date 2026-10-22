// Schéma des zones de choc 1 à 8. Les positions des pastilles devront suivre ZONES_CHOC
// (1 = avant/latéral gauche… 8 = avant) : la maquette les place librement.
export function VehicleDiagram() {
  return <div className="vehicle-diagram"><div className="car-body"><div className="windshield" /><div className="cabin" />{[1, 2, 3, 4, 5, 6, 7, 8].map(n => <span key={n} className={`zone z${n} ${n === 7 ? "selected" : ""}`}>{n}</span>)}</div><div className="diagram-caption"><strong>Zone 7 · Arrière</strong><span>Zone de choc principale</span></div></div>;
}
