import { z } from 'zod';

export const ProjectFileSchema = z.object({
  id: z.string().uuid(),
  projectId: z.string().uuid(),
  originalName: z.string(),
  mimeType: z.string(),
  sizeBytes: z.number(),
  createdAt: z.string(),
});

export type ProjectFileModel = z.infer<typeof ProjectFileSchema>;