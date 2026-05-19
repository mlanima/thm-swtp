import {z} from 'zod';
/** Validation of required project information from the wizard steps.
 * The schemas are used to validate the user input in all wizard steps before submitting.
 */


/**
 * Validation of general project information.
 * 'name' is required. Must contain at least 3 characters and must not exceed 20 characters.
 * 'description' is optional. Must not exceed 500 characters.
 */
export const projectGeneralSchema = z.object({
  name: z.string()
    .trim()
    .min(3, 'Project name must be at least 3 characters.')
    .max(20, 'Project name must be shorter then 20 characters.'),

  description: z.string()
    .trim()
    .max(500, 'Project description must be shorter then 500 characters.')
    .optional()
    .or(z.literal('')),
});

/**
 * Validation of project settings information.
 * 'projectUrl' is required. Must contain at least 3 characters and must not exceed 30 characters. Must only obtain lowercase, number and hyphens.
 * 'isPrivateProfile' must be a boolean value.
 */
export const projectSettingsSchema = z.object({
  projectUrl: z.string()
    .trim()
    .min(3, 'Project url must be at least 3 characters.')
    .max(30, 'Project url must be shorter then 30 characters.')
    .regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/,'Only use lowercase letters, numbers and hyphens. Hyphens are not allowed at the beginning or end.'),

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
