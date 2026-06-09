import { apiClient } from "@/api/client";
import type { Composition, CompositionDraft } from "@/features/compositions/types";

export async function getCompositions(params?: { page?: number; size?: number }) {
  const response = await apiClient.get<Composition[]>("/api/v1/composition", {
    params: {
      page: params?.page ?? 0,
      size: params?.size ?? 20,
    },
  });
  return response.data;
}

export async function getComposition(id: number) {
  const response = await apiClient.get<Composition>(`/api/v1/composition/${id}`);
  return response.data;
}

export async function createComposition(payload: CompositionDraft) {
  const response = await apiClient.post<Composition>("/api/v1/composition", payload);
  return response.data;
}

export async function updateComposition(id: number, payload: CompositionDraft) {
  const response = await apiClient.put<Composition>(`/api/v1/composition/${id}`, payload);
  return response.data;
}

export async function deleteComposition(id: number) {
  await apiClient.delete(`/api/v1/composition/${id}`);
}

export async function requestCompositionGrading(id: number) {
  const response = await apiClient.post<Composition>(`/api/v1/composition/${id}/grading`);
  return response.data;
}

export async function generateComposition(payload: Pick<CompositionDraft, "title" | "language">) {
  const response = await apiClient.post<Composition>("/api/v1/composition/generate", {
    ...payload,
    compositionSource: "SYSTEM",
  });
  return response.data;
}
