import { Link, NavLink, useNavigate } from "react-router-dom";
import { BookOpenCheck, FileText, LayoutDashboard, LogOut, PenLine } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { APP_NAME } from "@/lib/constants";
import { cn, getInitials } from "@/lib/utils";
import { useAuthStore } from "@/store/auth-store";

const mobileNavigation = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { label: "Compositions", href: "/compositions", icon: FileText },
  { label: "New", href: "/compositions/new", icon: PenLine },
];

export function Topbar() {
  const navigate = useNavigate();
  const username = useAuthStore((state) => state.username);
  const role = useAuthStore((state) => state.role);
  const logout = useAuthStore((state) => state.logout);

  function handleLogout() {
    logout();
    navigate("/login", { replace: true });
  }

  return (
    <header className="sticky top-0 z-20 border-b bg-background/95 backdrop-blur">
      <div className="flex h-16 items-center justify-between px-4 lg:px-8">
        <Link to="/dashboard" className="flex items-center gap-3 lg:hidden">
          <div className="rounded-lg bg-primary p-2 text-primary-foreground">
            <BookOpenCheck className="h-5 w-5" aria-hidden="true" />
          </div>
          <span className="max-w-[190px] truncate text-sm font-semibold">{APP_NAME}</span>
        </Link>

        <div className="hidden lg:block">
          <p className="text-sm text-muted-foreground">Workspace</p>
        </div>

        <div className="flex items-center gap-3">
          <div className="hidden text-right sm:block">
            <p className="text-sm font-medium">{username ?? "Learner"}</p>
            <p className="text-xs text-muted-foreground">{role ?? "User"}</p>
          </div>
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-secondary text-sm font-semibold">
            {getInitials(username)}
          </div>
          <Button variant="ghost" size="icon" type="button" onClick={handleLogout} aria-label="Log out">
            <LogOut className="h-4 w-4" aria-hidden="true" />
          </Button>
        </div>
      </div>

      <Separator className="lg:hidden" />

      <nav className="grid grid-cols-3 gap-1 px-2 py-2 lg:hidden">
        {mobileNavigation.map((item) => (
          <NavLink
            key={item.href}
            to={item.href}
            className={({ isActive }) =>
              cn(
                "flex items-center justify-center gap-2 rounded-md px-2 py-2 text-xs font-medium text-muted-foreground",
                isActive && "bg-secondary text-foreground",
              )
            }
          >
            <item.icon className="h-4 w-4" aria-hidden="true" />
            {item.label}
          </NavLink>
        ))}
      </nav>
    </header>
  );
}
