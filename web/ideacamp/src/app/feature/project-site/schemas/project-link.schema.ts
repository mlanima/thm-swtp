import { z } from 'zod';

const containsNoQuotes = (value : string) => !value.includes('"') && !value.includes("'");

export const createProjectLinkSchema = z.object({
  label: z.string()
    .trim()
    .min(1, "Bezeichnung erforderlich.")
    .max(100, "Die Bezeichnung darf nicht länger als 100 Zeichen sein.")
    .refine(containsNoQuotes, "Die Bezeichnung darf keine Anführungszeichen enthalten."),

  url: z.string()
    .trim()
    .min(1, "URL erforderlich.")
    .max(300, "Die URL darf nicht länger als 300 Zeichen sein.")
    .url("Die URL muss gültig sein."),
});

export const updateProjectLinkSchema = z.object({
  label: z.string()
    .trim()
    .max(100, 'Die Bezeichnung darf nicht länger als 100 Zeichen sein.')
    .refine(containsNoQuotes, 'Die Bezeichnung darf keine Anführungszeichen enthalten.')
    .optional(),

  url: z.string()
    .trim()
    .max(300, 'Die URL darf nicht länger als 300 Zeichen sein.')
    .url('Die URL muss gültig sein.')
    .optional(),
});

export type CreateProjectLinkRequest = z.infer<typeof createProjectLinkSchema>;
export type UpdateProjectLinkRequest = z.infer<typeof updateProjectLinkSchema>;

