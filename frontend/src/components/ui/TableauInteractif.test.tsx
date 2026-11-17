import { useState } from "react";
import { act, cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ETAT_INITIAL, parametresTableau, TableauInteractif, type Colonne, type EtatTableau } from "./TableauInteractif";

afterEach(() => {
  cleanup();
  vi.useRealTimers();
});

interface Ligne { piece: string; prix: number }
const LIGNES: Ligne[] = [{ piece: "Phare", prix: 300 }, { piece: "Pare-choc", prix: 1000 }];
const COLONNES: Colonne<Ligne>[] = [
  { cle: "piece", titre: "PIÈCE", texte: (l) => l.piece },
  { cle: "prix", titre: "PRIX", texte: (l) => String(l.prix), numerique: true },
];

/** Rend le tableau piloté comme par une page ; renvoie l'historique des états demandés. */
function rendre(total = 60) {
  const etats: EtatTableau[] = [];
  function Hote() {
    const [etat, setEtat] = useState(ETAT_INITIAL);
    return <TableauInteractif colonnes={COLONNES} lignes={LIGNES} total={total} etat={etat}
      changer={(e) => { etats.push(e); setEtat(e); }} cleLigne={(l) => l.piece} />;
  }
  render(<Hote />);
  return etats;
}
const entetes = () => screen.getAllByRole("columnheader").slice(0, 2).map((th) => th.textContent?.replace(/[▲▼⇅]/g, ""));

describe("tableau interactif (paginé par le serveur)", () => {
  it("demande le tri au serveur : croissant, décroissant puis aucun, en revenant à la première page", () => {
    const etats = rendre();
    fireEvent.click(screen.getByRole("button", { name: "2" }));
    const prix = screen.getByRole("button", { name: /PRIX/ });

    fireEvent.click(prix);
    fireEvent.click(prix);
    fireEvent.click(prix);

    expect(etats.map((e) => [e.page, e.tri])).toEqual([
      [1, null], [0, { cle: "prix", sens: "asc" }], [0, { cle: "prix", sens: "desc" }], [0, null]]);
  });

  it("envoie les filtres de colonne 300 ms après la frappe", () => {
    vi.useFakeTimers();
    const etats = rendre();

    fireEvent.change(screen.getByLabelText("Filtrer PIÈCE"), { target: { value: "ph" } });
    fireEvent.change(screen.getByLabelText("Filtrer PIÈCE"), { target: { value: "phare" } });
    expect(etats).toHaveLength(0);
    act(() => { vi.advanceTimersByTime(300); });

    expect(etats).toEqual([{ ...ETAT_INITIAL, filtres: { piece: "phare" } }]);
  });

  it("pagine : pages, taille de page et lignes affichées", () => {
    const etats = rendre(60);

    expect(screen.getByText("1–25 sur 60")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Page suivante" }));
    expect(screen.getByText("26–50 sur 60")).toBeTruthy();
    fireEvent.change(screen.getByLabelText("Lignes par page"), { target: { value: "50" } });

    expect(etats.at(-1)).toMatchObject({ page: 0, taille: 50 });
    expect(screen.getByText("1–50 sur 60")).toBeTruthy();
  });

  it("déplace une colonne par glisser-déposer de son en-tête, ou au clavier", () => {
    rendre();
    const [piece, prix] = screen.getAllByRole("columnheader");

    fireEvent.dragStart(prix);
    fireEvent.dragOver(piece);
    fireEvent.drop(piece);
    expect(entetes()).toEqual(["PRIX", "PIÈCE"]);
    expect(within(screen.getAllByRole("row")[2]).getAllByRole("cell")[0].textContent).toBe("300");

    fireEvent.keyDown(screen.getByRole("button", { name: /PRIX/ }), { key: "ArrowRight", altKey: true });
    expect(entetes()).toEqual(["PIÈCE", "PRIX"]);
  });

  it("traduit l'état en paramètres de requête", () => {
    expect(parametresTableau({ page: 2, taille: 50, tri: { cle: "prix", sens: "desc" }, filtres: { piece: " phare ", prix: "" } }))
      .toEqual({ page: 2, taille: 50, tri: "prix,desc", f_piece: "phare" });
  });
});
