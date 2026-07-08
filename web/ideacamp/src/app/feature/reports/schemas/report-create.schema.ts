import { z } from 'zod';
import { REPORT_REASONS } from '../models/report-shared.model';

const REPORT_TARGETS = ['USER', 'PROJECT', 'PROJECT_POST'] as const;

export const reportTargetSchema = z.enum(REPORT_TARGETS);

export const reportReasonSchema = z.enum(REPORT_REASONS);

export const createReportSchema = z.object({
  target: reportTargetSchema,
  targetId: z.uuid('REPORT_DIALOG.VALIDATION.TARGET_INVALID'),
  reason: reportReasonSchema,
  message: z.string().trim().max(1000, 'REPORT_DIALOG.VALIDATION.MESSAGE_MAX').optional(),
});

export type CreateReportFormValue = z.infer<typeof createReportSchema>;
