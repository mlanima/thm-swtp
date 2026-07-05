import { z } from 'zod';

export const UserSearchResultSchema = z.object({
  keycloakId: z.string().uuid(),
  username: z.string(),
  title: z.string().nullable(),
  location: z.string().nullable(),
});

export type UserSearchResult = z.infer<typeof UserSearchResultSchema>;
