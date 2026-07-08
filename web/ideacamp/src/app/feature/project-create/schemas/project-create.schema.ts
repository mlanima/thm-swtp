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
    .min(3, 'PROJECTCREATE.VALIDATION.NAME_MIN')
    .max(20, 'PROJECTCREATE.VALIDATION.NAME_MAX')
    .refine(containsNoQuotes, 'PROJECTCREATE.VALIDATION.NAME_NO_QUOTES'),

  shortDescription: z.string()
    .trim()
    .max(200, 'PROJECTCREATE.VALIDATION.SHORT_DESCRIPTION_MAX')
    .refine(containsNoQuotes, 'PROJECTCREATE.VALIDATION.SHORT_DESCRIPTION_NO_QUOTES')
    .optional()
    .or(z.literal('')),

  description: z.string()
    .trim()
    .max(500, 'PROJECTCREATE.VALIDATION.DESCRIPTION_MAX')
    .refine(containsNoQuotes, 'PROJECTCREATE.VALIDATION.DESCRIPTION_NO_QUOTES')
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
    .min(3, 'PROJECTCREATE.VALIDATION.PROJECT_URL_MIN')
    .max(30, 'PROJECTCREATE.VALIDATION.PROJECT_URL_MAX')
    .regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/,'PROJECTCREATE.VALIDATION.PROJECT_URL_FORMAT'),

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
