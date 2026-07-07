import { z } from 'zod';

export const ProjectSearchResultSchema = z.object({
  id: z.string().uuid(),
  name: z.string(),
  shortDescription: z.string().nullable(),
  description: z.string().nullable(),
  projectUrl: z.string(),
});

export type ProjectSearchResult = z.infer<typeof ProjectSearchResultSchema>;
