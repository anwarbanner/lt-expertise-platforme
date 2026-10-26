import type { ReactNode } from "react";

/** Champ en lecture seule ; « warning » = à saisir (F5), « error » = contrôle en échec (P4). */
export function Field({ label, value, state }: { label: string; value: string; state?: "warning" | "error" }) {
  return <label className={`data-field ${state || ""}`}><span>{label}</span><input value={value} placeholder={state === "warning" ? "À saisir" : ""} readOnly />{state === "warning" && <small>À saisir</small>}{state === "error" && <small>Valeur incohérente</small>}</label>;
}

type Option = readonly [string, string];

/**
 * Champ modifiable de l'écran de vérification. La valeur est une chaîne (vide = non renseigné) ; {@code options}
 * en fait une liste déroulante, {@code type="textarea"} une zone de texte. « warning » = absent du PDF : signalé,
 * la saisie reste facultative (sauf données essentielles, voir le panneau des contrôles).
 */
export function ChampSaisie({ label, value, onChange, type = "text", options, state, disabled, aide }: {
  label: string;
  value: string;
  onChange: (valeur: string) => void;
  type?: "text" | "date" | "number" | "textarea";
  options?: readonly Option[];
  state?: "warning" | "error";
  disabled?: boolean;
  aide?: ReactNode;
}) {
  const commun = { value, disabled, "aria-invalid": state === "error" || undefined };
  return <label className={`data-field ${state || ""} ${type === "textarea" ? "large" : ""}`}><span>{label}</span>
    {options
      ? <select {...commun} onChange={(e) => onChange(e.target.value)}><option value="">—</option>{options.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select>
      : type === "textarea"
        ? <textarea {...commun} rows={4} onChange={(e) => onChange(e.target.value)} />
        : <input {...commun} type={type} step={type === "number" ? "0.01" : undefined} placeholder={state === "warning" ? "Non renseigné" : ""} onChange={(e) => onChange(e.target.value)} />}
    {state === "warning" && <small>Absent du PDF</small>}{state === "error" && <small>Contrôle en échec</small>}{aide && !state && <small className="aide">{aide}</small>}
  </label>;
}
