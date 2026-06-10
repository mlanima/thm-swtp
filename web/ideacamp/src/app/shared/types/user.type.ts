import { z } from 'zod';

// Zod schema for a User with only `username` field
export const UserSchema = z.object({
  username: z.string().min(1, { message: 'Nutzername ist erforderlich.' }),
  id:z.string().min(1, { message: 'ID ist erforderlich' }),
});

export type User = z.infer<typeof UserSchema>;

