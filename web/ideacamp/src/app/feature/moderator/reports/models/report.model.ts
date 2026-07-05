export type ReportTarget = 'USER'| 'PROJECT' | 'PROJECT_POST';

export type ReportStatus = 'OPEN' | 'IN_REVIEW' | 'RESOLVED' | 'DISMISSED';

export type ReportReason = | 'SPAM' | 'HARASSMENT' | 'HATE_SPEECH' | 'INAPPROPRIATE_CONTENT' | 'VIOLENCE_OR_THREATS'
  | 'SELF_HARM_OR_SUICIDE' | 'PERSONAL_DATA' | 'COPYRIGHT' | 'MISINFORMATION' | 'FRAUD_OR_IMPERSONATION' | 'OTHER';

export interface ManagedReport {
  id: string;
  reporterId: string;
  reporterUsername: string;
  target: ReportTarget;
  targetId: string;
  reason: ReportReason;
  message: string | null;
  status: ReportStatus;
  reviewerKeycloakId: string;
  reviewerUsername: string;
  reviewedAt: string | null;
  moderatorMessage: string | null;
  targetSummary: ReportTargetSummary | null;
  createdAt: string;
  updatedAt: string;
  similarReportsCount: number;
}

export type ReportSortField = 'reason' | 'target' | 'status' | 'reporter.username' | 'reviewerUsername' | 'reviewedAt' | 'createdAt' | 'updatedAt';

export type SortDirection = 'asc' | 'desc';

export interface UpdateReportStatus {
  status: ReportStatus;
  moderatorMessage?: string;
}

export interface ReportSearchParams {
  page: number;
  size: number;
  status?: ReportStatus;
  target?: ReportTarget;
  reason?: ReportReason;
  query?: string;
  sortField?: ReportSortField;
  sortDirection?: SortDirection;
}

export interface ReportTargetSummary{
  title: string;
  subtitle: string;
  link: string | null;
  parentId: string | null;
}

export type ReportPriority = 'CRITICAL' | 'MEDIUM' | 'LOW';


export type TargetAction = 'BAN_USER' | 'DELETE_PROJECT' | 'DELETE_PROJECT_POST';
