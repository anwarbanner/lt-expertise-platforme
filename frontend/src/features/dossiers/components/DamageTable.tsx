import { DataTable } from "@/components/ui/DataTable";

/** Évaluation des dommages : les 7 postes de main d'œuvre (evaluation_main_oeuvre) et les fournitures. */
export function DamageTable({ editable = true }: { editable?: boolean }) {
  const rows = [["MO 1 Tôlerie", "20", "60,00", "1 200,00", "20%", "1 440,00"], ["MO 2 Tôlerie", "4", "60,00", "240,00", "20%", "288,00"], ["MO 3 Tôlerie", "—", "60,00", "0,00", "20%", "0,00"], ["MO Peinture", "8", "60,00", "480,00", "20%", "576,00"], ["MO 1 Mécanique", "2", "60,00", "120,00", "20%", "144,00"], ["MO 2 Mécanique", "—", "60,00", "0,00", "20%", "0,00"], ["MO 3 Mécanique", "—", "60,00", "0,00", "20%", "0,00"]];
  return <><DataTable headers={["POSTE", "NBR H", "PRIX UNIT.", "TOTAL HT", "TVA", "TOTAL TTC"]}>{rows.map(r => <tr key={r[0]}>{r.map((v, i) => <td key={i} className={i > 0 ? "money" : ""}>{editable && i > 0 ? <input value={v} readOnly /> : i === 0 ? <strong>{v}</strong> : v}</td>)}</tr>)}</DataTable><div className="damage-totals"><div><span>Pièces</span><b>400,00 DH</b></div><div><span>Ingrédient peinture</span><b>200,00 DH</b></div><div><span>Petite fourniture</span><b>80,00 DH</b></div><div className="grand"><span>Total général</span><b>2 400,00 DH</b></div></div></>;
}
