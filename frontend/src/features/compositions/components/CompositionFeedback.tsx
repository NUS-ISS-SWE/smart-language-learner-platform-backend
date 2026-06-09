import { Sparkles } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

type CompositionFeedbackProps = {
  feedback?: string | null;
};

export function CompositionFeedback({ feedback }: CompositionFeedbackProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Sparkles className="h-5 w-5 text-primary" aria-hidden="true" />
          Feedback
        </CardTitle>
        <CardDescription>AI grading feedback returned by the backend.</CardDescription>
      </CardHeader>
      <CardContent>
        {feedback?.trim() ? (
          <div className="whitespace-pre-wrap rounded-lg border bg-secondary/40 p-4 text-sm leading-7">
            {feedback}
          </div>
        ) : (
          <div className="rounded-lg border border-dashed bg-secondary/30 p-6 text-sm text-muted-foreground">
            No feedback is available yet. Submit the composition for grading to request review.
          </div>
        )}
      </CardContent>
    </Card>
  );
}
