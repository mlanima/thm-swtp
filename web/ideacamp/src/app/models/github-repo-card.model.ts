import { z } from 'zod';

export const LanguageShareSchema = z.object({
  name: z.string(),
  percentage: z.number(),
});

export const GithubRepoCardSchema = z.object({
  repoOwner: z.string(),
  repoName: z.string(),
  htmlUrl: z.string().nullable(),
  description: z.string().nullable(),
  stargazersCount: z.number().nullable(),
  forksCount: z.number().nullable(),
  languages: z.array(LanguageShareSchema),
  dataUnavailable: z.boolean(),
  showReadme: z.boolean(),
});

export type GithubRepoCardModel = z.infer<typeof GithubRepoCardSchema>;
