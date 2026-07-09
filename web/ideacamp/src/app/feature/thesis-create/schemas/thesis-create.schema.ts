import {z} from 'zod';
import {containsNoQuotes} from '../../project-create/schemas/project-create.schema';

/** Validation of required thesis information from the wizard steps.
 * The schemas are used to validate the user input in all wizard steps before submitting.
 * Limits mirror the backend's CreateThesisRequest validation.
 */

/**
 * Validation of general thesis information.
 * 'title' is required. Must contain at least 3 characters and must not exceed 100 characters.
 * 'shortDescription' is optional. Must not exceed 300 characters.
 * 'description' is optional. Must not exceed 2000 characters.
 * All must not contain quotes.
 */
export const thesisGeneralSchema = z.object({
  title: z.string()
    .trim()
    .min(3, 'THESISCREATE.VALIDATION.TITLE_MIN')
    .max(100, 'THESISCREATE.VALIDATION.TITLE_MAX')
    .refine(containsNoQuotes, 'THESISCREATE.VALIDATION.TITLE_NO_QUOTES'),

  shortDescription: z.string()
    .trim()
    .max(300, 'THESISCREATE.VALIDATION.SHORT_DESCRIPTION_MAX')
    .refine(containsNoQuotes, 'THESISCREATE.VALIDATION.SHORT_DESCRIPTION_NO_QUOTES')
    .optional()
    .or(z.literal('')),

  description: z.string()
    .trim()
    .max(2000, 'THESISCREATE.VALIDATION.DESCRIPTION_MAX')
    .refine(containsNoQuotes, 'THESISCREATE.VALIDATION.DESCRIPTION_NO_QUOTES')
    .optional()
    .or(z.literal('')),
});

/**
 * Validation of thesis settings information.
 * 'thesisUrl' is required. Must contain at least 3 characters and must not exceed 30 characters. Must only contain lowercase, number and hyphens.
 */
export const thesisSettingsSchema = z.object({
  thesisUrl: z.string()
    .trim()
    .min(3, 'THESISCREATE.VALIDATION.THESIS_URL_MIN')
    .max(30, 'THESISCREATE.VALIDATION.THESIS_URL_MAX')
    .regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/, 'THESISCREATE.VALIDATION.THESIS_URL_FORMAT'),
});

/** Combines all schemas from the steps into one validation schema.
 *  It is used to make sure that all required thesis data from the wizard is complete and valid.
 */
export const thesisCreateSchema = thesisGeneralSchema
  .extend(thesisSettingsSchema.shape)

export type ThesisGeneralData = z.infer<typeof thesisGeneralSchema>;
export type ThesisSettingsData = z.infer<typeof thesisSettingsSchema>;
export type ThesisCreateData = z.infer<typeof thesisCreateSchema>;
