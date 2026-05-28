import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { forgotPassword } from "@/api/auth.api";
import { getApiErrorMessage } from "@/api/client";
import { ErrorState } from "@/components/shared/ErrorState";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export function ForgotPasswordPage() {
  const [email, setEmail] = useState("");

  const mutation = useMutation({
    mutationFn: forgotPassword,
  });

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    mutation.mutate({ email });
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Reset password</CardTitle>
        <CardDescription>Enter your account email to request a password reset.</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="space-y-5" onSubmit={handleSubmit}>
          {mutation.isError ? <ErrorState message={getApiErrorMessage(mutation.error)} /> : null}

          {mutation.isSuccess ? (
            <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700">
              If the email exists, a reset request has been submitted.
            </div>
          ) : null}

          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </div>

          <Button className="w-full" type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            Send reset request
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-muted-foreground">
          Remembered your password?{" "}
          <Link to="/login" className="font-medium text-primary hover:underline">
            Back to login
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}
