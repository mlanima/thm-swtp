import { z } from 'zod';
import { FileVisibilitySchema } from './file-visibility.model';

export const ProjectFileSchema = z.object({
  id: z.string().uuid(),
  projectId: z.string().uuid(),
  originalName: z.string(),
  mimeType: z.string(),
  sizeBytes: z.number(),
  createdAt: z.string(),
  visibility: FileVisibilitySchema,
});

export type ProjectFileModel = z.infer<typeof ProjectFileSchema>;
