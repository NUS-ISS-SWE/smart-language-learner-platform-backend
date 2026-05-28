export type LoginPayload = {
  username: string;
  password: string;
};

export type RegisterPayload = {
  username: string;
  email: string;
  password: string;
  preferredLanguage: "ENGLISH" | "CHINESE";
  difficultyLevel: "BEGINNER" | "INTERMEDIATE" | "ADVANCED";
};

export type ForgotPasswordPayload = {
  email: string;
};

export type JwtAccessToken = {
  accessToken: string;
  role: string;
};

export type AppUser = {
  id?: number;
  username: string;
  email: string;
  role?: string;
  status?: string;
  preferredLanguage?: string;
  difficultyLevel?: string;
};
