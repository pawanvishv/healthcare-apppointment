import { z } from "zod";

const envSchema = z.object({
  NEXT_PUBLIC_API_BASE_URL: z.string().url(),
  NEXT_PUBLIC_APP_ENV: z.string().default("development"),
});

export const env = envSchema.parse({
  NEXT_PUBLIC_API_BASE_URL:
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1",
  NEXT_PUBLIC_APP_ENV: process.env.NEXT_PUBLIC_APP_ENV ?? "development",
});
