/** Champ de formulaire en lecture seule ; « warning » = à saisir (F5), « error » = contrôle en échec (P4). */
export function Field({ label, value, state }: { label: string; value: string; state?: "warning" | "error" }) {
  return <label className={`data-field ${state || ""}`}><span>{label}</span><input value={value} placeholder={state === "warning" ? "À saisir" : ""} readOnly />{state === "warning" && <small>À saisir</small>}{state === "error" && <small>Valeur incohérente</small>}</label>;
}

export function Filter({ label, value }: { label: string; value: string }) {
  return <label className="filter"><span>{label}</span><button>{value}<span>⌄</span></button></label>;
}
