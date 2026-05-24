import { z } from 'zod';

// Zod schema for a User with only `username` field
export const UserSchema = z.object({
  username: z.string().min(1, { message: 'username is required' }),
});

export type User = z.infer<typeof UserSchema>;

