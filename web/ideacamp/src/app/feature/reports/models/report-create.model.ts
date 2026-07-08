import { ReportReason, ReportTarget} from './report-shared.model';
export { REPORT_REASONS } from './report-shared.model';
export type { ReportReason, ReportTarget } from './report-shared.model';

export interface CreateReportRequest {
  target: ReportTarget;
  targetId: string;
  reason: ReportReason;
  message?: string | null;
}
