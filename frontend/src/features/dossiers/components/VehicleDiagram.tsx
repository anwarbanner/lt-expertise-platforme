import { ZONES_CHOC } from "@/domain/referentiels";

/**
 * Schéma des zones de choc 1 à 8. Positions : 8 avant, 1-2 avant gauche / droite, 3-4 centre gauche / droite,
 * 5-6 arrière gauche / droite, 7 arrière (codes de ZoneChoc côté backend). Un rapport peut toucher plusieurs zones ;
 * avec {@code changer}, un clic sur une zone l'ajoute ou la retire.
 */
const POSITIONS: Record<number, { top?: number; bottom?: number; left?: number; right?: number }> = {
  8: { top: -10, left: 30 }, 1: { top: 35, left: -10 }, 2: { top: 35, right: -10 }, 3: { top: 84, left: -10 },
  4: { top: 84, right: -10 }, 5: { bottom: 27, left: -10 }, 6: { bottom: 27, right: -10 }, 7: { bottom: -10, left: 30 },
};

export function VehicleDiagram({ zones, changer }: { zones: number[]; changer?: (zones: number[]) => void }) {
  const basculer = (code: number) => changer?.(zones.includes(code) ? zones.filter((z) => z !== code) : [...zones, code].sort((a, b) => a - b));
  return <div className="vehicle-diagram"><div className="car-body"><div className="windshield" /><div className="cabin" />
    {Object.entries(POSITIONS).map(([code, position]) => {
      const n = Number(code);
      const classe = `zone ${zones.includes(n) ? "selected" : ""}`;
      return changer
        ? <button type="button" key={code} className={classe} style={position} title={ZONES_CHOC[n]} aria-pressed={zones.includes(n)} aria-label={`Zone ${n} · ${ZONES_CHOC[n]}`} onClick={() => basculer(n)}>{code}</button>
        : <span key={code} className={classe} style={position} title={ZONES_CHOC[n]}>{code}</span>;
    })}
  </div><div className="diagram-caption">{zones.length
    ? <><strong>{zones.map((z) => `${z} · ${ZONES_CHOC[z]}`).join(" — ")}</strong><span>{zones.length > 1 ? `${zones.length} zones de choc` : "Zone de choc"}{changer ? " · cliquez une zone pour l’ajouter ou la retirer" : ""}</span></>
    : <span>Zone de choc non renseignée{changer ? " · cliquez une zone pour la sélectionner" : ""}</span>}</div></div>;
}
