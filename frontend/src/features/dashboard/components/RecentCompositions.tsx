import { Link } from "react-router-dom";
import { ArrowRight } from "lucide-react";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { formatDate } from "@/lib/utils";
import type { Composition } from "@/features/compositions/types";

type RecentCompositionsProps = {
  compositions: Composition[];
};

export function RecentCompositions({ compositions }: RecentCompositionsProps) {
  const recent = compositions.slice(0, 5);

  return (
    <Card>
      <CardHeader className="flex flex-row items-start justify-between gap-4">
        <div>
          <CardTitle>Recent compositions</CardTitle>
          <CardDescription>Your latest drafts and reviewed work.</CardDescription>
        </div>
        <Button variant="outline" size="sm" asChild>
          <Link to="/compositions">
            View all
            <ArrowRight className="h-4 w-4" aria-hidden="true" />
          </Link>
        </Button>
      </CardHeader>
      <CardContent>
        {recent.length === 0 ? (
          <p className="rounded-lg border border-dashed p-6 text-sm text-muted-foreground">
            No compositions yet.
          </p>
        ) : (
          <div className="divide-y rounded-lg border">
            {recent.map((composition) => (
              <Link
                key={composition.id}
                to={`/compositions/${composition.id}`}
                className="flex items-center justify-between gap-4 p-4 transition-colors hover:bg-secondary/60"
              >
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{composition.title || "Untitled"}</p>
                  <p className="mt-1 text-xs text-muted-foreground">{formatDate(composition.updatedAt)}</p>
                </div>
                <StatusBadge status={composition.compositionStatus} />
              </Link>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
