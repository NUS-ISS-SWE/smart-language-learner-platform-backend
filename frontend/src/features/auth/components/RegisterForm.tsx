import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { register } from "@/api/auth.api";
import { getApiErrorMessage } from "@/api/client";
import { ErrorState } from "@/components/shared/ErrorState";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import type { RegisterPayload } from "@/features/auth/types";

const defaultForm: RegisterPayload = {
  username: "",
  email: "",
  password: "",
  preferredLanguage: "ENGLISH",
  difficultyLevel: "INTERMEDIATE",
};

export function RegisterForm() {
  const navigate = useNavigate();
  const [form, setForm] = useState<RegisterPayload>(defaultForm);

  const mutation = useMutation({
    mutationFn: register,
    onSuccess: () => {
      navigate("/register/success", { replace: true, state: { email: form.email } });
    },
  });

  function updateField<Key extends keyof RegisterPayload>(key: Key, value: RegisterPayload[Key]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    mutation.mutate(form);
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Create account</CardTitle>
        <CardDescription>Set up your learner profile and start a composition workspace.</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="space-y-5" onSubmit={handleSubmit}>
          {mutation.isError ? <ErrorState message={getApiErrorMessage(mutation.error)} /> : null}

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="username">Username</Label>
              <Input
                id="username"
                autoComplete="username"
                value={form.username}
                onChange={(event) => updateField("username", event.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                value={form.email}
                onChange={(event) => updateField("email", event.target.value)}
                required
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              type="password"
              autoComplete="new-password"
              minLength={6}
              value={form.password}
              onChange={(event) => updateField("password", event.target.value)}
              required
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="preferredLanguage">Preferred language</Label>
              <Select
                id="preferredLanguage"
                value={form.preferredLanguage}
                onChange={(event) =>
                  updateField("preferredLanguage", event.target.value as RegisterPayload["preferredLanguage"])
                }
              >
                <option value="ENGLISH">English</option>
                <option value="CHINESE">Chinese</option>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="difficultyLevel">Difficulty</Label>
              <Select
                id="difficultyLevel"
                value={form.difficultyLevel}
                onChange={(event) =>
                  updateField("difficultyLevel", event.target.value as RegisterPayload["difficultyLevel"])
                }
              >
                <option value="BEGINNER">Beginner</option>
                <option value="INTERMEDIATE">Intermediate</option>
                <option value="ADVANCED">Advanced</option>
              </Select>
            </div>
          </div>

          <Button className="w-full" type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            Create account
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-muted-foreground">
          Already have an account?{" "}
          <Link to="/login" className="font-medium text-primary hover:underline">
            Log in
          </Link>
        </p>
      </CardContent>
    </Card>
  );
}
