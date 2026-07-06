import { z } from 'zod';

export const GithubReadmeSchema = z.object({
  repoOwner: z.string(),
  repoName: z.string(),
  defaultBranch: z.string(),
  markdown: z.string().nullable(),
  available: z.boolean(),
});

export type GithubReadmeModel = z.infer<typeof GithubReadmeSchema>;
