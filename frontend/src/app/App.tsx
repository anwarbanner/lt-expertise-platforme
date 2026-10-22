import { BrowserRouter, Navigate, Route, Routes } from "react-router";
import { AppLayout } from "./layout/AppLayout";
import { ROUTES } from "./routes";
import ConnexionPage from "@/features/auth/ConnexionPage";
import TableauDeBordPage from "@/features/dashboard/TableauDeBordPage";
import ImportPage from "@/features/imports/ImportPage";
import DossiersPage from "@/features/dossiers/DossiersPage";
import VerificationPage from "@/features/dossiers/VerificationPage";
import DossierDetailPage from "@/features/dossiers/DossierDetailPage";
import AnalysePrixPage from "@/features/analyse-prix/AnalysePrixPage";
import GenerationPage from "@/features/generation/GenerationPage";
import UtilisateursPage from "@/features/utilisateurs/UtilisateursPage";

/** Arbre des routes, séparé du routeur pour pouvoir être rendu dans un MemoryRouter (tests). */
export function RoutesApplication() {
  return <Routes>
      <Route path={ROUTES.connexion} element={<ConnexionPage />} />
      <Route element={<AppLayout />}>
        <Route index element={<TableauDeBordPage />} />
        <Route path={ROUTES.imports} element={<ImportPage />} />
        <Route path={ROUTES.dossiers} element={<DossiersPage />} />
        <Route path="/dossiers/:numero" element={<DossierDetailPage />} />
        <Route path="/dossiers/:numero/verification" element={<VerificationPage />} />
        <Route path={ROUTES.analysePrix} element={<AnalysePrixPage />} />
        <Route path={ROUTES.generation} element={<GenerationPage />} />
        <Route path={ROUTES.utilisateurs} element={<UtilisateursPage />} />
      </Route>
      <Route path="*" element={<Navigate to={ROUTES.tableauDeBord} replace />} />
    </Routes>;
}

export default function App() {
  return <BrowserRouter><RoutesApplication /></BrowserRouter>;
}
