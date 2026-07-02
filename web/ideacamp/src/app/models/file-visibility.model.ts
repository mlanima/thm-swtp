import { z } from 'zod';

export const FileVisibilitySchema = z.enum(['PUBLIC', 'PRIVATE']);
export type FileVisibility = z.infer<typeof FileVisibilitySchema>;