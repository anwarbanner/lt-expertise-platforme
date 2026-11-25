import { useState } from "react";
import { QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Navigate, Route, Routes } from "react-router";
import { creerQueryClient } from "@/api/queryClient";
import { AuthProvider } from "@/features/auth/AuthProvider";
import { RouteProtegee } from "@/features/auth/RouteProtegee";
import ConnexionPage from "@/features/auth/ConnexionPage";
import TableauDeBordPage from "@/features/dashboard/TableauDeBordPage";
import ImportPage from "@/features/imports/ImportPage";
import DossiersPage from "@/features/dossiers/DossiersPage";
import VerificationPage from "@/features/dossiers/VerificationPage";
import DossierDetailPage from "@/features/dossiers/DossierDetailPage";
import AnalysePrixPage from "@/features/analyse-prix/AnalysePrixPage";
import ValeursVenalesPage from "@/features/recherche/ValeursVenalesPage";
import GenerationPage from "@/features/generation/GenerationPage";
import UtilisateursPage from "@/features/utilisateurs/UtilisateursPage";
import { AppLayout } from "./layout/AppLayout";
import { ROLES_PAR_ECRAN, ROUTES } from "./routes";

/** Arbre des routes, séparé du routeur pour pouvoir être rendu dans un MemoryRouter (tests). */
export function RoutesApplication() {
  return <Routes>
    <Route path={ROUTES.connexion} element={<ConnexionPage />} />
    <Route element={<RouteProtegee><AppLayout /></RouteProtegee>}>
      <Route index element={<TableauDeBordPage />} />
      <Route path={ROUTES.imports} element={<RouteProtegee roles={ROLES_PAR_ECRAN.imports}><ImportPage /></RouteProtegee>} />
      <Route path={ROUTES.dossiers} element={<DossiersPage />} />
      <Route path="/dossiers/:numero" element={<DossierDetailPage />} />
      <Route path="/dossiers/:numero/verification" element={<RouteProtegee roles={ROLES_PAR_ECRAN.verification}><VerificationPage /></RouteProtegee>} />
      <Route path={ROUTES.analysePrix} element={<AnalysePrixPage />} />
      <Route path={ROUTES.recherchePieces} element={<Navigate to={ROUTES.analysePrix} replace />} />
      <Route path={ROUTES.valeursVenales} element={<ValeursVenalesPage />} />
      <Route path={ROUTES.generation} element={<GenerationPage />} />
      <Route path={ROUTES.utilisateurs} element={<RouteProtegee roles={ROLES_PAR_ECRAN.utilisateurs}><UtilisateursPage /></RouteProtegee>} />
    </Route>
    <Route path="*" element={<Navigate to={ROUTES.tableauDeBord} replace />} />
  </Routes>;
}

export default function App() {
  const [queryClient] = useState(creerQueryClient);
  return <QueryClientProvider client={queryClient}>
    <AuthProvider>
      <BrowserRouter><RoutesApplication /></BrowserRouter>
    </AuthProvider>
  </QueryClientProvider>;
}
