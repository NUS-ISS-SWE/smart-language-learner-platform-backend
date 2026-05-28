import { AlertCircle } from "lucide-react";
import { cn } from "@/lib/utils";

type ErrorStateProps = {
  message: string;
  className?: string;
};

export function ErrorState({ message, className }: ErrorStateProps) {
  return (
    <div className={cn("rounded-lg border border-destructive/20 bg-destructive/5 p-4 text-sm text-destructive", className)}>
      <div className="flex gap-2">
        <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
        <span>{message}</span>
      </div>
    </div>
  );
}
