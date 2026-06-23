import { z } from 'zod';
import { LinkVisibilitySchema} from '../../../models/link-visibility.model';

const containsNoQuotes = (value : string) => !value.includes('"') && !value.includes("'");

export const createProjectLinkSchema = z.object({
  label: z.string()
    .trim()
    .min(1, 'PROJECTSITE.QUICKLINKS.VALIDATION_LABEL_REQUIRED')
    .max(100, 'PROJECTSITE.QUICKLINKS.VALIDATION_LABEL_TOO_LONG')
    .refine(containsNoQuotes, 'PROJECTSITE.QUICKLINKS.VALIDATION_LABEL_NO_QUOTES'),

  url: z.string()
    .trim()
<<<<<<< HEAD
    .min(1, "URL erforderlich.")
    .max(300, "Die URL darf nicht länger als 300 Zeichen sein.")
    .url("Die URL muss gültig sein."),

  visibility: LinkVisibilitySchema.default('PUBLIC'),
=======
    .min(1, 'PROJECTSITE.QUICKLINKS.VALIDATION_URL_REQUIRED')
    .max(300, 'PROJECTSITE.QUICKLINKS.VALIDATION_URL_TOO_LONG')
    .url('PROJECTSITE.QUICKLINKS.VALIDATION_URL_INVALID'),
>>>>>>> origin/developer
});

export const updateProjectLinkSchema = z.object({
  label: z
    .string()
    .trim()
    .max(100, 'PROJECTSITE.QUICKLINKS.VALIDATION_LABEL_TOO_LONG')
    .refine(containsNoQuotes, 'PROJECTSITE.QUICKLINKS.VALIDATION_LABEL_NO_QUOTES')
    .optional(),

  url: z
    .string()
    .trim()
    .max(300, 'PROJECTSITE.QUICKLINKS.VALIDATION_URL_TOO_LONG')
    .url('PROJECTSITE.QUICKLINKS.VALIDATION_URL_INVALID')
    .optional(),

  visibility: LinkVisibilitySchema.optional(),
});

export type CreateProjectLinkRequest = z.infer<typeof createProjectLinkSchema>;
export type UpdateProjectLinkRequest = z.infer<typeof updateProjectLinkSchema>;

