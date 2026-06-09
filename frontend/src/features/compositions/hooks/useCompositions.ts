import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createComposition,
  deleteComposition,
  generateComposition,
  getComposition,
  getCompositions,
  requestCompositionGrading,
  updateComposition,
} from "@/api/composition.api";
import type { CompositionDraft } from "@/features/compositions/types";

export const compositionKeys = {
  all: ["compositions"] as const,
  lists: () => [...compositionKeys.all, "list"] as const,
  list: (page: number, size: number) => [...compositionKeys.lists(), page, size] as const,
  details: () => [...compositionKeys.all, "detail"] as const,
  detail: (id: number) => [...compositionKeys.details(), id] as const,
};

export function useCompositionList(page = 0, size = 20) {
  return useQuery({
    queryKey: compositionKeys.list(page, size),
    queryFn: () => getCompositions({ page, size }),
  });
}

export function useComposition(id: number | null) {
  return useQuery({
    queryKey: id ? compositionKeys.detail(id) : compositionKeys.details(),
    queryFn: () => getComposition(id as number),
    enabled: id !== null,
  });
}

export function useCreateComposition() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createComposition,
    onSuccess: (composition) => {
      void queryClient.invalidateQueries({ queryKey: compositionKeys.lists() });
      if (composition.id) {
        queryClient.setQueryData(compositionKeys.detail(composition.id), composition);
      }
    },
  });
}

export function useUpdateComposition(id: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CompositionDraft) => updateComposition(id, payload),
    onSuccess: (composition) => {
      void queryClient.invalidateQueries({ queryKey: compositionKeys.lists() });
      if (composition.id) {
        queryClient.setQueryData(compositionKeys.detail(composition.id), composition);
      }
    },
  });
}

export function useDeleteComposition() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteComposition,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: compositionKeys.lists() });
    },
  });
}

export function useRequestGrading() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: requestCompositionGrading,
    onSuccess: (composition) => {
      void queryClient.invalidateQueries({ queryKey: compositionKeys.lists() });
      if (composition.id) {
        queryClient.setQueryData(compositionKeys.detail(composition.id), composition);
      }
    },
  });
}

export function useGenerateComposition() {
  return useMutation({
    mutationFn: generateComposition,
  });
}
