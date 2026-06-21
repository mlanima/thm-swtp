import { z } from 'zod';

// Zod schema for a User with only `username` field
export const UserSchema = z.object({
  username: z.string().min(1, { message: 'USER.VALIDATION.USERNAME_REQUIRED' }),
  id:z.string().min(1, { message: 'USER.VALIDATION.ID_REQUIRED' }),
});

export type User = z.infer<typeof UserSchema>;

