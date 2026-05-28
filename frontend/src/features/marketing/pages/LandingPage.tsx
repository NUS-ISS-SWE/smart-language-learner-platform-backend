import { Link } from "react-router-dom";
import {
  ArrowRight,
  BookOpenCheck,
  FileText,
  LineChart,
  PenLine,
  Sparkles,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import landingHero from "@/assets/landing-hero.png";
import { APP_NAME } from "@/lib/constants";
import { useAuthStore } from "@/store/auth-store";

const features = [
  {
    title: "Composition workspace",
    description: "Draft English or Chinese compositions in a clean editor built for focused writing.",
    icon: PenLine,
  },
  {
    title: "AI-guided review",
    description: "Request grading and read structured feedback tied to the submitted composition.",
    icon: Sparkles,
  },
  {
    title: "Learning history",
    description: "Return to previous drafts, reviewed work, and writing progress from one dashboard.",
    icon: LineChart,
  },
];

export function LandingPage() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  return (
    <main className="min-h-screen bg-background text-foreground">
      <header className="sticky top-0 z-30 border-b bg-white/95 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <Link to="/" className="flex items-center gap-3">
            <div className="rounded-lg bg-primary p-2 text-primary-foreground">
              <BookOpenCheck className="h-5 w-5" aria-hidden="true" />
            </div>
            <span className="text-sm font-semibold sm:text-base">{APP_NAME}</span>
          </Link>

          <nav className="flex items-center gap-2">
            {isAuthenticated ? (
              <Button asChild>
                <Link to="/dashboard">
                  Dashboard
                  <ArrowRight className="h-4 w-4" aria-hidden="true" />
                </Link>
              </Button>
            ) : (
              <>
                <Button variant="ghost" asChild>
                  <Link to="/login">Log in</Link>
                </Button>
                <Button variant="accent" asChild>
                  <Link to="/register">Sign up</Link>
                </Button>
              </>
            )}
          </nav>
        </div>
      </header>

      <section className="border-b bg-[linear-gradient(180deg,#ffffff_0%,#f8f9ff_100%)]">
        <div className="mx-auto grid min-h-[calc(100vh-4rem)] max-w-7xl items-center gap-10 px-4 py-12 sm:px-6 lg:grid-cols-[minmax(0,0.95fr)_minmax(420px,1.05fr)] lg:px-8">
          <div>
            <div className="max-w-3xl">
              <p className="text-sm font-semibold uppercase tracking-[0.18em] text-secondary">
                Professional composition learning
              </p>
              <h1 className="mt-5 text-4xl font-bold leading-tight tracking-normal text-balance sm:text-5xl">
                Smart Language Learner Platform
              </h1>
              <p className="mt-5 max-w-2xl text-lg leading-8 text-muted-foreground">
                A focused writing environment for learners to create compositions, receive AI-assisted feedback,
                and improve language expression with a clear academic workflow.
              </p>
              <div className="mt-8 flex flex-col gap-3 sm:flex-row">
                <Button size="lg" variant="accent" asChild>
                  <Link to={isAuthenticated ? "/dashboard" : "/register"}>
                    Start writing
                    <ArrowRight className="h-4 w-4" aria-hidden="true" />
                  </Link>
                </Button>
                <Button size="lg" variant="outline" asChild>
                  <Link to={isAuthenticated ? "/compositions" : "/login"}>Open workspace</Link>
                </Button>
              </div>
            </div>
          </div>

          <div>
            <img
              src={landingHero}
              alt="AI composition learning workspace with writing feedback cards"
              className="w-full rounded-xl border bg-white object-cover"
            />
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div className="max-w-2xl">
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-secondary">Platform workflow</p>
          <h2 className="mt-3 text-3xl font-bold tracking-normal">Built around writing improvement</h2>
        </div>

        <div className="mt-8 grid gap-4 md:grid-cols-3">
          {features.map((feature) => (
            <article key={feature.title} className="rounded-lg border bg-white p-6">
              <div className="mb-5 flex h-10 w-10 items-center justify-center rounded-lg bg-[#d3e4fe] text-primary">
                <feature.icon className="h-5 w-5" aria-hidden="true" />
              </div>
              <h3 className="text-lg font-semibold">{feature.title}</h3>
              <p className="mt-3 text-sm leading-6 text-muted-foreground">{feature.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="border-t bg-white">
        <div className="mx-auto flex max-w-7xl flex-col justify-between gap-5 px-4 py-10 sm:px-6 md:flex-row md:items-center lg:px-8">
          <div>
            <h2 className="text-2xl font-bold tracking-normal">Ready to start a composition?</h2>
            <p className="mt-2 text-sm text-muted-foreground">Create a learner account or continue to your workspace.</p>
          </div>
          <Button variant="accent" asChild>
            <Link to={isAuthenticated ? "/compositions/new" : "/register"}>
              <FileText className="h-4 w-4" aria-hidden="true" />
              New composition
            </Link>
          </Button>
        </div>
      </section>
    </main>
  );
}
