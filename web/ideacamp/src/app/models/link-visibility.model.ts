import { z } from 'zod';

export const LinkVisibilitySchema = z.enum(['PUBLIC', 'PRIVATE']);
export type LinkVisibility = z.infer<typeof LinkVisibilitySchema>;
