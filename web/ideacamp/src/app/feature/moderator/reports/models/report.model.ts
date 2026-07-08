import type { ReportReason, ReportTarget } from '../../../reports/models/report-shared.model';
export type { ReportReason, ReportTarget } from '../../../reports/models/report-shared.model';

export type ReportStatus = 'OPEN' | 'IN_REVIEW' | 'RESOLVED' | 'DISMISSED';

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

export type ReportSortField =
  | 'reason'
  | 'target'
  | 'status'
  | 'reporter.username'
  | 'reviewerUsername'
  | 'reviewedAt'
  | 'createdAt'
  | 'updatedAt';

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

export interface ReportTargetSummary {
  title: string;
  subtitle: string;
  link: string | null;
  parentId: string | null;
}

export type ReportPriority = 'CRITICAL' | 'MEDIUM' | 'LOW';
export type TargetAction = 'BAN_USER' | 'DELETE_PROJECT' | 'DELETE_PROJECT_POST';
