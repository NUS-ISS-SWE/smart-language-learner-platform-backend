export type CompositionSource = "RAW_TEXT" | "FILE_PDF" | "FILE_DOCX" | "SYSTEM";
export type CompositionLanguage = "ENGLISH" | "CHINESE";
export type CompositionStatus = "DRAFT" | "GENERATING" | "GENERATION_FAILED" | "REVIEWING" | "REVIEWED";

export type Composition = {
  id?: number;
  username?: string;
  title: string;
  content: string;
  compositionSource?: CompositionSource;
  language: CompositionLanguage;
  compositionStatus?: CompositionStatus;
  systemFeedback?: string | null;
  storagePath?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
};

export type CompositionDraft = {
  title: string;
  content: string;
  language: CompositionLanguage;
  compositionSource: CompositionSource;
};
