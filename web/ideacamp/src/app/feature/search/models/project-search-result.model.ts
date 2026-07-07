import { z } from 'zod';

export const ProjectSearchResultSchema = z.object({
  id: z.string().uuid(),
  name: z.string(),
  shortDescription: z.string().nullable(),
  description: z.string().nullable(),
  projectUrl: z.string(),
  tags: z.array(z.string()),
  openPositionsCount: z.number(),
  allowJoinRequests: z.boolean(),
});

export type ProjectSearchResult = z.infer<typeof ProjectSearchResultSchema>;
