import { useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getApiErrorMessage } from "@/api/client";
import { ErrorState } from "@/components/shared/ErrorState";
import { LoadingState } from "@/components/shared/LoadingState";
import { CompositionEditor } from "@/features/compositions/components/CompositionEditor";
import {
  useComposition,
  useCreateComposition,
  useUpdateComposition,
} from "@/features/compositions/hooks/useCompositions";
import type { CompositionDraft } from "@/features/compositions/types";

export function CompositionEditorPage() {
  const navigate = useNavigate();
  const params = useParams();
  const compositionId = useMemo(() => {
    const rawId = params.id ? Number(params.id) : null;
    return rawId && !Number.isNaN(rawId) ? rawId : null;
  }, [params.id]);

  const compositionQuery = useComposition(compositionId);
  const createMutation = useCreateComposition();
  const updateMutation = useUpdateComposition(compositionId ?? 0);
  const isEditing = compositionId !== null;

  function handleSave(draft: CompositionDraft) {
    if (compositionId) {
      updateMutation.mutate(draft, {
        onSuccess: (composition) => navigate(`/compositions/${composition.id}`),
      });
      return;
    }

    createMutation.mutate(draft, {
      onSuccess: (composition) => navigate(`/compositions/${composition.id}`),
    });
  }

  if (isEditing && compositionQuery.isLoading) {
    return <LoadingState label="Loading composition" />;
  }

  if (isEditing && compositionQuery.isError) {
    return <ErrorState message={getApiErrorMessage(compositionQuery.error)} />;
  }

  return (
    <div className="space-y-6">
      <div>
        <p className="text-sm font-medium text-primary">{isEditing ? "Edit composition" : "New composition"}</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-normal">
          {isEditing ? "Update your draft" : "Start writing"}
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Compose in a clean editor and save your work to the backend composition service.
        </p>
      </div>

      <CompositionEditor
        initialValue={compositionQuery.data}
        isSaving={createMutation.isPending || updateMutation.isPending}
        saveError={createMutation.error ?? updateMutation.error}
        onSave={handleSave}
      />
    </div>
  );
}
