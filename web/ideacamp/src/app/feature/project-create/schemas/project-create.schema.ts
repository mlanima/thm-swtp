import {z} from 'zod';
/** Validation of required project information from the wizard steps.
 * The schemas are used to validate the user input in all wizard steps before submitting.
 */

const containsNoQuotes = (value:string) => !value.includes('"') && !value.includes("'");

/**
 * Validation of general project information.
 * 'name' is required. Must contain at least 3 characters and must not exceed 20 characters.
 * 'description' is optional. Must not exceed 500 characters.
 * Both must not contain quotes.
 */
export const projectGeneralSchema = z.object({
  name: z.string()
    .trim()
    .min(3, 'Projektname muss mindestens 3 Zeichen lang sein.')
    .max(20, 'Projektname darf höchstens 20 Zeichen lang sein.')
    .refine(containsNoQuotes, 'Projektname darf keine Anführungszeichen beinhalten.'),

  shortDescription: z.string()
    .trim()
    .max(200, 'Kurzbeschreibung darf höchstens 200 Zeichen lang sein.')
    .refine(containsNoQuotes, 'Kurzbeschreibung darf keine Anführungszeichen beinhalten.')
    .optional()
    .or(z.literal('')),

  description: z.string()
    .trim()
    .max(500, 'Projektbeschreibung darf höchstens 500 Zeichen lang sein.')
    .refine(containsNoQuotes, 'Projektbeschreibung darf keine Anführungszeichen beinhalten.')
    .optional()
    .or(z.literal('')),
});

/**
 * Validation of project settings information.
 * 'projectUrl' is required. Must contain at least 3 characters and must not exceed 30 characters. Must only contain lowercase, number and hyphens.
 * 'isPrivateProfile' must be a boolean value.
 */
export const projectSettingsSchema = z.object({
  projectUrl: z.string()
    .trim()
    .min(3, 'Projekt URL muss mindestens 3 Zeichen lang sein.')
    .max(30, 'Projekt URL darf höchstens 30 Zeichen lang sein')
    .regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/,'Verwende ausschließlich Kleinbuchstaben, Zahlen und Bindestriche. Bindestriche sind am Anfang und am Ende nicht zulässig.'),

  isPrivateProject: z.boolean(),
});

/** Combines all schemas from the steps into one validation schema.
 *  It is used to make sure that all required project data from the wizard is complete and valid.
 */
export const projectCreateSchema = projectGeneralSchema
  .extend(projectSettingsSchema.shape)


export type ProjectGeneralData = z.infer<typeof projectGeneralSchema>;
export type ProjectSettingsData = z.infer<typeof projectSettingsSchema>;
export type ProjectCreateData = z.infer<typeof projectCreateSchema>;
