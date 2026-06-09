import { CheckCircle2, Clock3, FileText } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import type { Composition } from "@/features/compositions/types";

type ScoreSummaryProps = {
  compositions: Composition[];
};

export function ScoreSummary({ compositions }: ScoreSummaryProps) {
  const reviewed = compositions.filter((item) => item.compositionStatus === "REVIEWED").length;
  const inProgress = compositions.filter(
    (item) => item.compositionStatus === "REVIEWING" || item.compositionStatus === "GENERATING",
  ).length;

  const items = [
    {
      label: "Total compositions",
      value: compositions.length,
      icon: FileText,
      tone: "text-primary bg-primary/10",
    },
    {
      label: "Reviewed",
      value: reviewed,
      icon: CheckCircle2,
      tone: "text-emerald-700 bg-emerald-100",
    },
    {
      label: "In progress",
      value: inProgress,
      icon: Clock3,
      tone: "text-amber-800 bg-amber-100",
    },
  ];

  return (
    <div className="grid gap-4 md:grid-cols-3">
      {items.map((item) => (
        <Card key={item.label}>
          <CardContent className="flex items-center gap-4 p-5">
            <div className={`rounded-lg p-3 ${item.tone}`}>
              <item.icon className="h-5 w-5" aria-hidden="true" />
            </div>
            <div>
              <p className="text-2xl font-semibold">{item.value}</p>
              <p className="text-sm text-muted-foreground">{item.label}</p>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
