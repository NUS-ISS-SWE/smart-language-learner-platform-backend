import { Link, useNavigate, useParams } from "react-router-dom";
import { useMemo } from "react";
import { ArrowLeft, Edit, Loader2, Trash2 } from "lucide-react";
import { getApiErrorMessage } from "@/api/client";
import { CompositionFeedback } from "@/features/compositions/components/CompositionFeedback";
import {
  useComposition,
  useDeleteComposition,
  useRequestGrading,
} from "@/features/compositions/hooks/useCompositions";
import { ErrorState } from "@/components/shared/ErrorState";
import { LoadingState } from "@/components/shared/LoadingState";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { formatDate } from "@/lib/utils";

export function CompositionDetailPage() {
  const navigate = useNavigate();
  const params = useParams();
  const compositionId = useMemo(() => {
    const rawId = Number(params.id);
    return Number.isNaN(rawId) ? null : rawId;
  }, [params.id]);

  const compositionQuery = useComposition(compositionId);
  const gradeMutation = useRequestGrading();
  const deleteMutation = useDeleteComposition();
  const composition = compositionQuery.data;

  function handleGrade() {
    if (!compositionId) {
      return;
    }
    gradeMutation.mutate(compositionId);
  }

  function handleDelete() {
    if (!compositionId || !window.confirm("Delete this composition?")) {
      return;
    }

    deleteMutation.mutate(compositionId, {
      onSuccess: () => navigate("/compositions", { replace: true }),
    });
  }

  if (compositionQuery.isLoading) {
    return <LoadingState label="Loading composition" />;
  }

  if (compositionQuery.isError || !composition) {
    return <ErrorState message={getApiErrorMessage(compositionQuery.error)} />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-start">
        <div>
          <Button variant="ghost" size="sm" asChild className="-ml-3 mb-2">
            <Link to="/compositions">
              <ArrowLeft className="h-4 w-4" aria-hidden="true" />
              Back
            </Link>
          </Button>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-3xl font-semibold tracking-normal">{composition.title || "Untitled"}</h1>
            <StatusBadge status={composition.compositionStatus} />
          </div>
          <p className="mt-2 text-sm text-muted-foreground">
            {composition.language === "CHINESE" ? "Chinese" : "English"} composition created{" "}
            {formatDate(composition.createdAt)}
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          <Button variant="outline" asChild>
            <Link to={`/compositions/${composition.id}/edit`}>
              <Edit className="h-4 w-4" aria-hidden="true" />
              Edit
            </Link>
          </Button>
          <Button variant="outline" type="button" onClick={handleGrade} disabled={gradeMutation.isPending}>
            {gradeMutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            Request grading
          </Button>
          <Button
            variant="destructive"
            type="button"
            onClick={handleDelete}
            disabled={deleteMutation.isPending}
            aria-label="Delete composition"
          >
            {deleteMutation.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Trash2 className="h-4 w-4" />
            )}
            Delete
          </Button>
        </div>
      </div>

      {gradeMutation.isError ? <ErrorState message={getApiErrorMessage(gradeMutation.error)} /> : null}
      {deleteMutation.isError ? <ErrorState message={getApiErrorMessage(deleteMutation.error)} /> : null}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_420px]">
        <Card>
          <CardHeader>
            <CardTitle>Composition</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="min-h-[460px] whitespace-pre-wrap rounded-lg border bg-white p-5 text-base leading-8">
              {composition.content}
            </div>
            <Separator className="my-5" />
            <div className="grid gap-3 text-sm text-muted-foreground sm:grid-cols-2">
              <span>Source: {composition.compositionSource ?? "RAW_TEXT"}</span>
              <span>Updated: {formatDate(composition.updatedAt)}</span>
            </div>
          </CardContent>
        </Card>

        <CompositionFeedback feedback={composition.systemFeedback} />
      </div>
    </div>
  );
}
