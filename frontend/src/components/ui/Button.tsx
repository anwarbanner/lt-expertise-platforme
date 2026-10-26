import type { ReactNode } from "react";
import { Icon, type IconName } from "./Icon";

export function Button({ children, variant = "primary", icon, onClick, disabled, title, type, form }: { children: ReactNode; variant?: "primary" | "secondary" | "danger" | "quiet"; icon?: IconName; onClick?: () => void; disabled?: boolean; title?: string; type?: "button" | "submit"; form?: string }) {
  return <button className={`button ${variant}`} type={type} form={form} onClick={onClick} disabled={disabled} title={title}>{icon && <Icon name={icon} size={15} />}{children}</button>;
}
