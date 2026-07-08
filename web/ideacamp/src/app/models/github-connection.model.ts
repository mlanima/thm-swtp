import { z } from 'zod';

export const GithubConnectionStatusSchema = z.object({
  enabled: z.boolean(),
  connected: z.boolean(),
  githubLogin: z.string().nullable(),
  avatarUrl: z.string().nullable(),
  status: z.string().nullable(),
});

export type GithubConnectionStatusModel = z.infer<typeof GithubConnectionStatusSchema>;

export const GithubAuthorizeUrlSchema = z.object({
  authorizeUrl: z.string().url(),
});
