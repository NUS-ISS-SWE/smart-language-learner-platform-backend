import { Link, Outlet } from "react-router-dom";
import { BookOpenCheck } from "lucide-react";
import { APP_NAME } from "@/lib/constants";

export function AuthLayout() {
  return (
    <main className="min-h-screen bg-background">
      <header className="border-b bg-white">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <Link to="/" className="flex items-center gap-3">
            <div className="rounded-lg bg-primary p-2 text-primary-foreground">
              <BookOpenCheck className="h-5 w-5" aria-hidden="true" />
            </div>
            <span className="text-sm font-semibold sm:text-base">{APP_NAME}</span>
          </Link>
        </div>
      </header>

      <section className="flex min-h-[calc(100vh-4rem)] items-center justify-center px-4 py-10">
        <div className="w-full max-w-md">
          <div className="mb-6 text-center">
            <div className="mx-auto mb-4 flex h-11 w-11 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <BookOpenCheck className="h-5 w-5" aria-hidden="true" />
            </div>
            <p className="text-sm font-medium text-muted-foreground">Smart Language Learner Platform</p>
          </div>
          <Outlet />
        </div>
      </section>
    </main>
  );
}
