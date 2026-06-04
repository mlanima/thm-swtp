import { z } from 'zod';

const containsNoQuotes = (value : string) => !value.includes('"') && !value.includes("'");

export const createProjectLinkSchema = z.object({
  label: z.string()
    .trim()
    .min(1, "Label is required.")
    .max(100, "Label must be shorter than 100 characters.")
    .refine(containsNoQuotes, "Label must not contain any quotes."),

  url: z.string()
    .trim()
    .min(1, "URL is required.")
    .max(300, "URL must be shorter than 300 characters.")
    .url("URL must be a valid URL."),
});

export const updateProjectLinkSchema = z.object({
  label: z.string()
    .trim()
    .max(100, 'Label must be shorter than 100 characters.')
    .refine(containsNoQuotes, 'Label must not contain any quotes.')
    .optional(),

  url: z.string()
    .trim()
    .max(300, 'URL must be shorter than 300 characters.')
    .url('URL must be a valid URL.')
    .optional(),
});

export type CreateProjectLinkRequest = z.infer<typeof createProjectLinkSchema>;
export type UpdateProjectLinkRequest = z.infer<typeof updateProjectLinkSchema>;

