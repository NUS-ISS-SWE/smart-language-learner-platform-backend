import { Link } from "react-router-dom";
import { PenLine } from "lucide-react";
import { ErrorState } from "@/components/shared/ErrorState";
import { LoadingState } from "@/components/shared/LoadingState";
import { Button } from "@/components/ui/button";
import { getApiErrorMessage } from "@/api/client";
import { useCompositionList } from "@/features/compositions/hooks/useCompositions";
import { RecentCompositions } from "@/features/dashboard/components/RecentCompositions";
import { ScoreSummary } from "@/features/dashboard/components/ScoreSummary";
import { useAuthStore } from "@/store/auth-store";

export function DashboardPage() {
  const username = useAuthStore((state) => state.username);
  const { data = [], isLoading, isError, error } = useCompositionList();

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="text-sm font-medium text-primary">Learner dashboard</p>
          <h1 className="mt-2 text-3xl font-semibold tracking-normal">Welcome, {username ?? "learner"}</h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">
            Review your writing activity, continue recent work, or start a new composition.
          </p>
        </div>
        <Button asChild>
          <Link to="/compositions/new">
            <PenLine className="h-4 w-4" aria-hidden="true" />
            New composition
          </Link>
        </Button>
      </div>

      {isLoading ? <LoadingState label="Loading dashboard" /> : null}
      {isError ? <ErrorState message={getApiErrorMessage(error)} /> : null}

      {!isLoading && !isError ? (
        <>
          <ScoreSummary compositions={data} />
          <RecentCompositions compositions={data} />
        </>
      ) : null}
    </div>
  );
}
