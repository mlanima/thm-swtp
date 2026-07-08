export const REPORT_TARGETS = ['USER', 'PROJECT', 'PROJECT_POST'] as const;

export type ReportTarget = (typeof REPORT_TARGETS)[number];

export const REPORT_REASONS = [
  'SPAM',
  'HARASSMENT',
  'HATE_SPEECH',
  'INAPPROPRIATE_CONTENT',
  'VIOLENCE_OR_THREATS',
  'SELF_HARM_OR_SUICIDE',
  'PERSONAL_DATA',
  'COPYRIGHT',
  'MISINFORMATION',
  'FRAUD_OR_IMPERSONATION',
  'OTHER',
] as const;

export type ReportReason = (typeof REPORT_REASONS)[number];
