import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { DataTable } from "@/components/ui/DataTable";
import { Icon } from "@/components/ui/Icon";
import { PageHead } from "@/components/ui/PageHead";
import { tarifs } from "@/mocks/donnees";

/**
 * Analyse des prix : écran absent du cahier des charges (F1–F14). Les données réelles viendraient de ligne_detail
 * (désignation, type O/A/R, prix unitaire) croisée avec la compagnie et le véhicule ; « référence » et
 * « fournisseur » n'existent pas dans les rapports PDF.
 */
export default function AnalysePrixPage() {
  const [type, setType] = useState("Tous");
  const [client, setClient] = useState("Tous les clients");
  const [vehicle, setVehicle] = useState("Tous les véhicules");
  const [query, setQuery] = useState("");
  const filtered = tarifs.filter(row => (type === "Tous" || row.type === type) && (client === "Tous les clients" || row.client === client) && (vehicle === "Tous les véhicules" || row.vehicle === vehicle) && (row.name.toLowerCase().includes(query.toLowerCase()) || row.reference.toLowerCase().includes(query.toLowerCase())));
  const prices = filtered.map(row => row.price);
  const minimum = prices.length ? Math.min(...prices) : 0;
  const maximum = prices.length ? Math.max(...prices) : 0;
  const average = prices.length ? Math.round(prices.reduce((sum, value) => sum + value, 0) / prices.length) : 0;
  const formatPrice = (value: number) => `${value.toLocaleString("fr-FR")} DH`;
  const reset = () => { setType("Tous"); setClient("Tous les clients"); setVehicle("Tous les véhicules"); setQuery(""); };
  return <main className="main pricing-page">
    <PageHead title="Analyse des prix" subtitle="Comparez les tarifs des pièces et fournitures par client et par véhicule" actions={<Button variant="secondary" icon="download">Exporter l’analyse</Button>} />
    <article className="card pricing-filters">
      <div className="pricing-search"><Icon name="search" size={17} /><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Rechercher une pièce, une fourniture ou une référence…" /></div>
      <label><span>Catégorie</span><select value={type} onChange={e => setType(e.target.value)}><option>Tous</option><option>Pièce</option><option>Fourniture</option></select></label>
      <label><span>Client / compagnie</span><select value={client} onChange={e => setClient(e.target.value)}><option>Tous les clients</option>{[...new Set(tarifs.map(row => row.client))].map(value => <option key={value}>{value}</option>)}</select></label>
      <label><span>Marque / modèle</span><select value={vehicle} onChange={e => setVehicle(e.target.value)}><option>Tous les véhicules</option>{[...new Set(tarifs.map(row => row.vehicle))].map(value => <option key={value}>{value}</option>)}</select></label>
      <Button variant="quiet" onClick={reset}>Réinitialiser</Button>
    </article>
    <section className="price-kpis">
      <article className="card price-kpi minimum"><span><Icon name="arrow" size={18} /></span><div><small>Prix minimum</small><strong>{formatPrice(minimum)}</strong><p>Tarif le plus bas observé</p></div></article>
      <article className="card price-kpi average"><span><Icon name="chart" size={18} /></span><div><small>Prix moyen</small><strong>{formatPrice(average)}</strong><p>Moyenne des résultats filtrés</p></div></article>
      <article className="card price-kpi maximum"><span><Icon name="arrow" size={18} /></span><div><small>Prix maximum</small><strong>{formatPrice(maximum)}</strong><p>Tarif le plus élevé observé</p></div></article>
      <article className="card price-kpi records"><span><Icon name="file" size={18} /></span><div><small>Tarifs analysés</small><strong>{filtered.length}</strong><p>Occurrences correspondantes</p></div></article>
    </section>
    <section className="pricing-insights">
      <article className="card price-range-card">
        <div className="section-head"><div><h2>Fourchette tarifaire</h2><p>Positionnement des tarifs sélectionnés</p></div><span className="price-spread">Écart : {formatPrice(maximum - minimum)}</span></div>
        <div className="range-visual"><div className="range-labels"><span>Minimum<b>{formatPrice(minimum)}</b></span><span>Moyenne<b>{formatPrice(average)}</b></span><span>Maximum<b>{formatPrice(maximum)}</b></span></div><div className="range-track"><i /><i /><i /></div></div>
        <div className="price-advice"><Icon name="check" size={16} /><div><strong>Indicateur de référence</strong><p>Un tarif proche de la moyenne facilite une estimation cohérente. Vérifiez les montants éloignés de plus de 20 %.</p></div></div>
      </article>
      <article className="card distribution-card"><div className="section-head"><div><h2>Répartition par catégorie</h2><p>Montant moyen par type</p></div></div><div className="distribution-bars"><div><span>Pièces</span><i><b className="parts-bar" /></i><strong>1 596 DH</strong></div><div><span>Fournitures</span><i><b className="supplies-bar" /></i><strong>325 DH</strong></div></div></article>
    </section>
    <article className="card pricing-results">
      <div className="section-head"><div><h2>Détail des tarifs</h2><p>{filtered.length} résultat{filtered.length !== 1 ? "s" : ""} correspondant aux critères</p></div><span className="results-updated"><Icon name="check" size={13} /> Données actualisées</span></div>
      <DataTable headers={["DÉSIGNATION", "TYPE", "RÉFÉRENCE", "CLIENT / COMPAGNIE", "VÉHICULE", "FOURNISSEUR", "PRIX TTC", "DATE"]}>{filtered.map((row, index) => <tr key={`${row.reference}-${index}`}><td><strong>{row.name}</strong></td><td><span className={`price-type ${row.type === "Pièce" ? "part" : "supply"}`}>{row.type}</span></td><td>{row.reference}</td><td>{row.client}</td><td>{row.vehicle}</td><td>{row.supplier}</td><td className="money"><strong>{formatPrice(row.price)}</strong></td><td>{row.date}</td></tr>)}</DataTable>
      {!filtered.length && <div className="empty-prices"><Icon name="search" size={24} /><strong>Aucun tarif trouvé</strong><span>Modifiez ou réinitialisez vos critères de recherche.</span></div>}
    </article>
  </main>;
}
