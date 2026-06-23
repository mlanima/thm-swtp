import { z } from 'zod';

export const UserProfileLinkSchema = z.object({
  id: z.uuid(),
  userProfileId: z.uuid(),
  label: z.string().max(100),
  url: z.url().max(300),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export type UserProfileLinkModel = z.infer<typeof UserProfileLinkSchema>;
