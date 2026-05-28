import { Badge } from "@/components/ui/badge";
import type { CompositionStatus } from "@/features/compositions/types";

type StatusBadgeProps = {
  status?: CompositionStatus | null;
};

export function StatusBadge({ status }: StatusBadgeProps) {
  const value = status ?? "DRAFT";

  if (value === "REVIEWED") {
    return <Badge variant="success">Reviewed</Badge>;
  }

  if (value === "REVIEWING" || value === "GENERATING") {
    return <Badge variant="warning">{value === "REVIEWING" ? "Reviewing" : "Generating"}</Badge>;
  }

  if (value === "GENERATION_FAILED") {
    return <Badge variant="destructive">Generation failed</Badge>;
  }

  return <Badge variant="secondary">Draft</Badge>;
}
