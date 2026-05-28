import { Link } from "react-router-dom";
import { Plus } from "lucide-react";
import { getApiErrorMessage } from "@/api/client";
import { EmptyState } from "@/components/shared/EmptyState";
import { ErrorState } from "@/components/shared/ErrorState";
import { LoadingState } from "@/components/shared/LoadingState";
import { Button } from "@/components/ui/button";
import { CompositionCard } from "@/features/compositions/components/CompositionCard";
import { useCompositionList } from "@/features/compositions/hooks/useCompositions";

export function CompositionListPage() {
  const { data = [], isLoading, isError, error } = useCompositionList();

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="text-sm font-medium text-primary">Composition library</p>
          <h1 className="mt-2 text-3xl font-semibold tracking-normal">Your compositions</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Manage drafts, generated work, and graded submissions.
          </p>
        </div>
        <Button asChild>
          <Link to="/compositions/new">
            <Plus className="h-4 w-4" aria-hidden="true" />
            New composition
          </Link>
        </Button>
      </div>

      {isLoading ? <LoadingState label="Loading compositions" /> : null}
      {isError ? <ErrorState message={getApiErrorMessage(error)} /> : null}

      {!isLoading && !isError && data.length === 0 ? (
        <EmptyState
          title="No compositions yet"
          description="Create your first composition, write manually, or generate a draft from a title."
          action={
            <Button asChild>
              <Link to="/compositions/new">Create composition</Link>
            </Button>
          }
        />
      ) : null}

      {!isLoading && !isError && data.length > 0 ? (
        <div className="grid gap-4 lg:grid-cols-2">
          {data.map((composition) => (
            <CompositionCard key={composition.id} composition={composition} />
          ))}
        </div>
      ) : null}
    </div>
  );
}
