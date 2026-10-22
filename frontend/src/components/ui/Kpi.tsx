import { Icon, type IconName } from "./Icon";

export function Kpi({ label, value, meta, icon, tone = "navy" }: { label: string; value: string; meta?: string; icon: IconName; tone?: string }) {
  return <article className="kpi"><div className={`kpi-icon ${tone}`}><Icon name={icon} size={20} /></div><div className="kpi-copy"><span>{label}</span><strong>{value}</strong>{meta && <small>{meta}</small>}</div></article>;
}
