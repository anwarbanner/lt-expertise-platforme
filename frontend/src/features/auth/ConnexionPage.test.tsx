import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";
import { AxiosError, AxiosHeaders } from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ROUTES } from "@/app/routes";
import { rendreApplication } from "@/test/rendu";

afterEach(cleanup);

function saisir(identifiant: string, motDePasse: string) {
  fireEvent.input(screen.getByPlaceholderText("Votre identifiant"), { target: { value: identifiant } });
  fireEvent.input(screen.getByPlaceholderText("Votre mot de passe"), { target: { value: motDePasse } });
  fireEvent.click(screen.getByRole("button", { name: /Se connecter/ }));
}

function erreurApi(statut: number, message: string) {
  const config = { headers: new AxiosHeaders() };
  return new AxiosError(message, String(statut), config, null, {
    status: statut, statusText: "", data: { message }, headers: {}, config,
  });
}

describe("page de connexion", () => {
  it("valide les champs sans appeler le serveur", async () => {
    const { auth } = rendreApplication(ROUTES.connexion, null);

    fireEvent.click(screen.getByRole("button", { name: /Se connecter/ }));

    expect(await screen.findByText("Saisissez votre identifiant.")).toBeTruthy();
    expect(screen.getByText("Saisissez votre mot de passe.")).toBeTruthy();
    expect(auth.connecter).not.toHaveBeenCalled();
  });

  it("envoie l'identifiant sans espaces et le mot de passe tel quel", async () => {
    const { auth } = rendreApplication(ROUTES.connexion, null);

    saisir("  gestionnaire ", " secret ");

    await waitFor(() => expect(auth.connecter).toHaveBeenCalledWith("gestionnaire", " secret "));
  });

  it.each([
    [401, "Identifiant ou mot de passe incorrect."],
    [423, "Compte verrouillé après plusieurs tentatives échouées. Réessayez après 10:45."],
  ])("affiche le message du serveur (%i)", async (statut, message) => {
    const { auth } = rendreApplication(ROUTES.connexion, null);
    vi.mocked(auth.connecter).mockRejectedValueOnce(erreurApi(statut, message));

    saisir("gestionnaire", "mauvais");

    expect((await screen.findByRole("alert")).textContent).toContain(message);
  });

  it("explique la réinitialisation par l'administrateur", () => {
    rendreApplication(ROUTES.connexion, null);

    fireEvent.click(screen.getByRole("button", { name: "Mot de passe oublié ?" }));

    expect(screen.getByText(/demandez à l’administrateur/)).toBeTruthy();
  });

  it("affiche ou masque le mot de passe", () => {
    rendreApplication(ROUTES.connexion, null);
    const champ = screen.getByPlaceholderText("Votre mot de passe") as HTMLInputElement;

    fireEvent.click(screen.getByRole("button", { name: "Afficher le mot de passe" }));

    expect(champ.type).toBe("text");
  });
});
