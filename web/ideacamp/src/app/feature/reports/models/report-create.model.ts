export type ReportTarget = 'USER' | 'PROJECT' | 'PROJECT_POST';

export type ReportReason =
  | 'SPAM'
  | 'HARASSMENT'
  | 'HATE_SPEECH'
  | 'INAPPROPRIATE_CONTENT'
  | 'VIOLENCE_OR_THREATS'
  | 'SELF_HARM_OR_SUICIDE'
  | 'PERSONAL_DATA'
  | 'COPYRIGHT'
  | 'MISINFORMATION'
  | 'FRAUD_OR_IMPERSONATION'
  | 'OTHER';

export interface CreateReportRequest {
  target: ReportTarget;
  targetId: string;
  reason: ReportReason;
  message?: string | null;
}

  export const REPORT_REASONS: ReportReason[] = [
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
  ];
