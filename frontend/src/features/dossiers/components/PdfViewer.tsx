import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { cles, pdfOriginal } from "@/api/dossiers";
import { messageErreur } from "@/api/client";
import { Icon } from "@/components/ui/Icon";

/** PDF original du dossier (P1), affiché par le lecteur du navigateur à partir du fichier relu et vérifié par le serveur. */
export function usePdfOriginal(importId: number | null) {
  const pdf = useQuery({ queryKey: cles.pdf(importId ?? 0), queryFn: () => pdfOriginal(importId!), enabled: importId !== null, staleTime: Infinity });
  const [adresse, setAdresse] = useState<string | null>(null);
  useEffect(() => {
    if (!pdf.data || typeof URL.createObjectURL !== "function") return;
    const url = URL.createObjectURL(pdf.data);
    setAdresse(url);
    return () => URL.revokeObjectURL(url);
  }, [pdf.data]);
  return { ...pdf, adresse };
}

export function PdfViewer({ importId, nomFichier }: { importId: number | null; nomFichier: string | null }) {
  const pdf = usePdfOriginal(importId);
  return <section className="pdf-panel">
    <div className="pdf-toolbar"><span>{nomFichier ?? "PDF original"}</span>
      <div>{pdf.adresse && <a className="pdf-ouvrir" href={pdf.adresse} target="_blank" rel="noreferrer">Ouvrir dans un onglet</a>}</div></div>
    <div className="pdf-canvas">
      {importId === null && <p className="pdf-message">Aucun PDF : dossier repris du fichier Excel.</p>}
      {pdf.isPending && importId !== null && <p className="pdf-message">Chargement du PDF…</p>}
      {pdf.isError && <p className="pdf-message">{messageErreur(pdf.error, "PDF indisponible.")}</p>}
      {pdf.adresse && <iframe className="pdf-cadre" title="PDF original" src={pdf.adresse} />}
    </div>
    <div className="pdf-footer"><Icon name="eye" size={14} /> Le PDF original fait foi (P1) : il n’est jamais modifié</div>
  </section>;
}
