import { useState } from "react";
import { Outlet } from "react-router";
import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";

export function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  return <div className={`app-shell ${collapsed ? "sidebar-collapsed" : ""}`}><Sidebar collapsed={collapsed} toggle={() => setCollapsed(!collapsed)} /><div className="workspace"><Topbar /><Outlet /></div></div>;
}
