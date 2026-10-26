import { useMutation } from "@tanstack/react-query";
import { messageErreur } from "@/api/client";
import { Button } from "./Button";
import type { IconName } from "./Icon";

/**
 * Bouton qui fait produire un fichier par le serveur (PDF, Excel) puis le télécharge ; affiche l'attente et le
 * motif d'un refus (ex. dossier non validé).
 */
export function BoutonTelechargement({ children, action, icon = "download", variant = "secondary", disabled, title, apres }: {
  children: string;
  action: () => Promise<string>;
  icon?: IconName;
  variant?: "primary" | "secondary" | "quiet";
  disabled?: boolean;
  title?: string;
  apres?: () => void;
}) {
  const telechargement = useMutation({ mutationFn: action, onSuccess: apres });
  return <span className="bouton-telechargement">
    <Button variant={variant} icon={icon} disabled={disabled || telechargement.isPending} title={title} onClick={() => telechargement.mutate()}>
      {telechargement.isPending ? "Préparation…" : children}
    </Button>
    {telechargement.isError && <small role="alert">{messageErreur(telechargement.error, "Téléchargement impossible.")}</small>}
  </span>;
}
