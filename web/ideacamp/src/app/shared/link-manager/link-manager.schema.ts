import { z } from 'zod';
import { LinkVisibilitySchema } from '../../models/link-visibility.model';

const containsNoQuotes = (value: string) => !value.includes('"') && !value.includes("'");

export const createLinkSchema = z.object({
  label: z
    .string()
    .trim()
    .min(1, 'COMMON.QUICKLINKS.VALIDATION_LABEL_REQUIRED')
    .max(100, 'COMMON.QUICKLINKS.VALIDATION_LABEL_TOO_LONG')
    .refine(containsNoQuotes, 'COMMON.QUICKLINKS.VALIDATION_LABEL_NO_QUOTES'),

  url: z
    .string()
    .trim()
    .min(1, 'COMMON.QUICKLINKS.VALIDATION_URL_REQUIRED')
    .max(300, 'COMMON.QUICKLINKS.VALIDATION_URL_TOO_LONG')
    .pipe(z.url('COMMON.QUICKLINKS.VALIDATION_URL_INVALID')),

  visibility: LinkVisibilitySchema.optional(),
});

export const updateLinkSchema = z.object({
  label: z
    .string()
    .trim()
    .max(100, 'COMMON.QUICKLINKS.VALIDATION_LABEL_TOO_LONG')
    .refine(containsNoQuotes, 'COMMON.QUICKLINKS.VALIDATION_LABEL_NO_QUOTES')
    .optional(),

  url: z
    .string()
    .trim()
    .max(300, 'COMMON.QUICKLINKS.VALIDATION_URL_TOO_LONG')
    .pipe(z.url('COMMON.QUICKLINKS.VALIDATION_URL_INVALID'))
    .optional(),

  visibility: LinkVisibilitySchema.optional(),
});

export type CreateLinkRequest = z.infer<typeof createLinkSchema>;
export type UpdateLinkRequest = z.infer<typeof updateLinkSchema>;
