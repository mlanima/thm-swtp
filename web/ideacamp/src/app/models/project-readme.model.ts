import { z } from 'zod';

export const ProjectReadmeSchema = z.object({
  repoUrl: z.string().url(),
  content: z.string(),
  owner: z.string(),
  repo: z.string(),
});

export type ProjectReadmeModel = z.infer<typeof ProjectReadmeSchema>;
