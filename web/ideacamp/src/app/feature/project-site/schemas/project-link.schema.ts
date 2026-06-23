import { z } from 'zod';
import { LinkVisibilitySchema} from '../../../models/link-visibility.model';

const containsNoQuotes = (value : string) => !value.includes('"') && !value.includes("'");

export const createProjectLinkSchema = z.object({
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
    .url('COMMON.QUICKLINKS.VALIDATION_URL_INVALID'),

  visibility: LinkVisibilitySchema.default('PUBLIC'),
});

export const updateProjectLinkSchema = z.object({
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
    .url('COMMON.QUICKLINKS.VALIDATION_URL_INVALID')
    .optional(),

  visibility: LinkVisibilitySchema.optional(),
});

export type CreateProjectLinkRequest = z.infer<typeof createProjectLinkSchema>;
export type UpdateProjectLinkRequest = z.infer<typeof updateProjectLinkSchema>;

