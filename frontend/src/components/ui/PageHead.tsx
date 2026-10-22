import type { ReactNode } from "react";

export function PageHead({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return <><div className="page-heading"><div><h1>{title}</h1>{subtitle && <p>{subtitle}</p>}</div><div className="head-actions">{actions}</div></div><div className="brand-rule"><i /><i /></div></>;
}
