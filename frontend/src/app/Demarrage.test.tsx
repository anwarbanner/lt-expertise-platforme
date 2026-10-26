import { cleanup, render, screen } from "@testing-library/react";
import MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import { api } from "@/api/client";
import App from "./App";

afterEach(cleanup);

/** L'application complète (fournisseurs, routeur, session) démarre et affiche la connexion sans session. */
describe("démarrage de l'application", () => {
  it("affiche la page de connexion quand aucune session n'existe", async () => {
    const serveur = new MockAdapter(api);
    serveur.onPost("/auth/rafraichir").reply(401);

    render(<App />);

    expect(await screen.findByRole("heading", { level: 1, name: "Bienvenue" })).toBeTruthy();
    expect(screen.getByPlaceholderText("Votre identifiant")).toBeTruthy();
    serveur.restore();
  });
});
