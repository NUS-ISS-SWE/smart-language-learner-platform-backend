import { Link } from "react-router-dom";
import { FileText } from "lucide-react";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { Card, CardContent } from "@/components/ui/card";
import { formatDate } from "@/lib/utils";
import type { Composition } from "@/features/compositions/types";

type CompositionCardProps = {
  composition: Composition;
};

export function CompositionCard({ composition }: CompositionCardProps) {
  const preview = composition.content?.trim()
    ? composition.content.trim().slice(0, 180)
    : "No composition content yet.";

  return (
    <Link to={`/compositions/${composition.id}`} className="block">
      <Card className="transition-colors hover:border-primary/35">
        <CardContent className="p-5">
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0">
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <FileText className="h-4 w-4" aria-hidden="true" />
                <span>{composition.language === "CHINESE" ? "Chinese" : "English"}</span>
              </div>
              <h3 className="mt-2 truncate text-base font-semibold">{composition.title || "Untitled"}</h3>
            </div>
            <StatusBadge status={composition.compositionStatus} />
          </div>

          <p className="mt-3 line-clamp-2 text-sm leading-6 text-muted-foreground">{preview}</p>

          <div className="mt-4 flex flex-wrap gap-x-4 gap-y-2 text-xs text-muted-foreground">
            <span>Created {formatDate(composition.createdAt)}</span>
            <span>Updated {formatDate(composition.updatedAt)}</span>
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
