import { Icon } from "@/components/ui/Icon";

// Aperçu factice : à remplacer par react-pdf alimenté par GET /api/imports/{id}/pdf (disponible).
// Le surlignage du champ sélectionné suppose que l'extraction enregistre la position de chaque valeur.
export function PdfViewer({ numero }: { numero: string }) {
  return <section className="pdf-panel"><div className="pdf-toolbar"><span>Rapport_{numero}.pdf</span><div><button>−</button><b>90%</b><button>+</button><i /><button>‹</button><b>1 / 2</b><button>›</button></div></div><div className="pdf-canvas"><div className="pdf-page"><img src="/logo-lt-expertise.png" alt="LT Expertise" /><h3>RAPPORT D’EXPERTISE AUTOMOBILE</h3><div className="pdf-ref">N° {numero}</div><div className="fake-lines">{Array.from({ length: 12 }).map((_, i) => <i key={i} style={{ width: `${55 + (i * 17) % 42}%` }} />)}</div><div className="pdf-highlight"><span>Montant de l’indemnisation</span><strong>2 400,00 DH</strong></div><div className="fake-table">{Array.from({ length: 20 }).map((_, i) => <i key={i} />)}</div></div></div><div className="pdf-footer"><Icon name="eye" size={14} /> Le champ sélectionné est surligné dans le document source</div></section>;
}
