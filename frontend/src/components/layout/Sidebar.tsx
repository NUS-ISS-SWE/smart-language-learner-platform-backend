import { Link, NavLink } from "react-router-dom";
import { BookOpenCheck, FileText, LayoutDashboard, PenLine } from "lucide-react";
import { APP_NAME } from "@/lib/constants";
import { cn } from "@/lib/utils";

const navigation = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "Compositions", href: "/compositions", icon: FileText },
  { label: "New Composition", href: "/compositions/new", icon: PenLine },
];

export function Sidebar() {
  return (
    <aside className="hidden bg-primary text-primary-foreground lg:flex lg:w-72 lg:flex-col">
      <Link to="/dashboard" className="flex h-16 items-center gap-3 border-b border-white/10 px-6">
        <div className="rounded-lg bg-white/10 p-2 text-white">
          <BookOpenCheck className="h-5 w-5" aria-hidden="true" />
        </div>
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-white">{APP_NAME}</p>
          <p className="text-xs text-white/60">Writing workspace</p>
        </div>
      </Link>

      <nav className="flex-1 space-y-1 px-3 py-4">
        {navigation.map((item) => (
          <NavLink
            key={item.href}
            to={item.href}
            className={({ isActive }) =>
              cn(
                "relative flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-white/70 transition-colors hover:bg-white/10 hover:text-white",
                isActive &&
                  "bg-white/10 pl-4 text-white before:absolute before:left-0 before:top-1.5 before:h-5 before:w-1 before:rounded-full before:bg-accent",
              )
            }
          >
            <item.icon className="h-4 w-4" aria-hidden="true" />
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
