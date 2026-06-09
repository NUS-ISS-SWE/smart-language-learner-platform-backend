import { FormEvent, useEffect, useState } from "react";
import { Loader2, Save, WandSparkles } from "lucide-react";
import { getApiErrorMessage } from "@/api/client";
import { ErrorState } from "@/components/shared/ErrorState";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { useGenerateComposition } from "@/features/compositions/hooks/useCompositions";
import type { Composition, CompositionDraft, CompositionLanguage } from "@/features/compositions/types";

type CompositionEditorProps = {
  initialValue?: Composition | null;
  isSaving: boolean;
  saveError?: unknown;
  onSave: (draft: CompositionDraft) => void;
};

const emptyDraft: CompositionDraft = {
  title: "",
  content: "",
  language: "ENGLISH",
  compositionSource: "RAW_TEXT",
};

export function CompositionEditor({ initialValue, isSaving, saveError, onSave }: CompositionEditorProps) {
  const [draft, setDraft] = useState<CompositionDraft>(emptyDraft);
  const generateMutation = useGenerateComposition();

  useEffect(() => {
    if (!initialValue) {
      setDraft(emptyDraft);
      return;
    }

    setDraft({
      title: initialValue.title ?? "",
      content: initialValue.content ?? "",
      language: initialValue.language ?? "ENGLISH",
      compositionSource: initialValue.compositionSource ?? "RAW_TEXT",
    });
  }, [initialValue]);

  function updateDraft<Key extends keyof CompositionDraft>(key: Key, value: CompositionDraft[Key]) {
    setDraft((current) => ({ ...current, [key]: value }));
  }

  function handleGenerate() {
    generateMutation.mutate(
      {
        title: draft.title,
        language: draft.language,
      },
      {
        onSuccess: (composition) => {
          setDraft((current) => ({
            ...current,
            content: composition.content ?? current.content,
            compositionSource: "SYSTEM",
          }));
        },
      },
    );
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSave(draft);
  }

  const canGenerate = draft.title.trim().length > 0 && !generateMutation.isPending;

  return (
    <form className="grid gap-6 xl:grid-cols-[1fr_320px]" onSubmit={handleSubmit}>
      <Card>
        <CardHeader>
          <CardTitle>Composition editor</CardTitle>
          <CardDescription>Write manually or generate a draft from the title and language.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-5">
          {saveError ? <ErrorState message={getApiErrorMessage(saveError)} /> : null}
          {generateMutation.isError ? <ErrorState message={getApiErrorMessage(generateMutation.error)} /> : null}

          <div className="space-y-2">
            <Label htmlFor="title">Title</Label>
            <Input
              id="title"
              value={draft.title}
              onChange={(event) => updateDraft("title", event.target.value)}
              placeholder="A memorable day at school"
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="content">Content</Label>
            <Textarea
              id="content"
              className="min-h-[460px] resize-y bg-white text-base leading-7"
              value={draft.content}
              onChange={(event) => updateDraft("content", event.target.value)}
              placeholder="Start writing your composition here..."
              required
            />
          </div>
        </CardContent>
      </Card>

      <aside className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>Settings</CardTitle>
            <CardDescription>These values are sent to the composition service.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-5">
            <div className="space-y-2">
              <Label htmlFor="language">Language</Label>
              <Select
                id="language"
                value={draft.language}
                onChange={(event) => updateDraft("language", event.target.value as CompositionLanguage)}
              >
                <option value="ENGLISH">English</option>
                <option value="CHINESE">Chinese</option>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="source">Source</Label>
              <Select
                id="source"
                value={draft.compositionSource}
                onChange={(event) =>
                  updateDraft("compositionSource", event.target.value as CompositionDraft["compositionSource"])
                }
              >
                <option value="RAW_TEXT">Manual text</option>
                <option value="SYSTEM">AI generated</option>
              </Select>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Actions</CardTitle>
            <CardDescription>Generate a draft or save your latest changes.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <Button
              type="button"
              variant="outline"
              className="w-full"
              onClick={handleGenerate}
              disabled={!canGenerate}
            >
              {generateMutation.isPending ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <WandSparkles className="h-4 w-4" />
              )}
              Generate draft
            </Button>

            <Button type="submit" className="w-full" disabled={isSaving}>
              {isSaving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
              Save composition
            </Button>
          </CardContent>
        </Card>
      </aside>
    </form>
  );
}
