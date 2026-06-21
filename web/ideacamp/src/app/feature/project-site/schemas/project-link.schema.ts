import { z } from 'zod';

const containsNoQuotes = (value : string) => !value.includes('"') && !value.includes("'");

export const createProjectLinkSchema = z.object({
  label: z.string()
    .trim()
    .min(1, 'PROJECTSITE.QUICKLINKS.VALIDATION_LABEL_REQUIRED')
    .max(100, 'PROJECTSITE.QUICKLINKS.VALIDATION_LABEL_TOO_LONG')
    .refine(containsNoQuotes, 'PROJECTSITE.QUICKLINKS.VALIDATION_LABEL_NO_QUOTES'),

  url: z.string()
    .trim()
    .min(1, 'PROJECTSITE.QUICKLINKS.VALIDATION_URL_REQUIRED')
    .max(300, 'PROJECTSITE.QUICKLINKS.VALIDATION_URL_TOO_LONG')
    .url('PROJECTSITE.QUICKLINKS.VALIDATION_URL_INVALID'),
});

export const updateProjectLinkSchema = z.object({
  label: z.string()
    .trim()
    .max(100, 'PROJECTSITE.QUICKLINKS.VALIDATION_LABEL_TOO_LONG')
    .refine(containsNoQuotes, 'PROJECTSITE.QUICKLINKS.VALIDATION_LABEL_NO_QUOTES')
    .optional(),

  url: z.string()
    .trim()
    .max(300, 'PROJECTSITE.QUICKLINKS.VALIDATION_URL_TOO_LONG')
    .url('PROJECTSITE.QUICKLINKS.VALIDATION_URL_INVALID')
    .optional(),
});

export type CreateProjectLinkRequest = z.infer<typeof createProjectLinkSchema>;
export type UpdateProjectLinkRequest = z.infer<typeof updateProjectLinkSchema>;

