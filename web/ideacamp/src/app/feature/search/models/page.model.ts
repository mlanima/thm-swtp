import { z } from 'zod';

export const PageSchema = <T extends z.ZodTypeAny>(item: T) =>
  z.object({
    content: z.array(item),
    totalElements: z.number(),
    totalPages: z.number(),
    number: z.number(),
    size: z.number(),
  });

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
