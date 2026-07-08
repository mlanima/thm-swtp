import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { DatePipe, NgClass } from '@angular/common';
import {
  ManagedReport, ReportReason,
  ReportSortField,
  ReportStatus, ReportTarget,
  SortDirection,
  ReportPriority
} from '../../models/report.model';
import { ReportActionMenuComponent } from '../report-action-menu/report-action-menu.component';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-report-table',
  standalone: true,
  imports: [DatePipe, TranslatePipe, ReportActionMenuComponent, RouterLink, NgClass],
  templateUrl: './report-table.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportTableComponent {
  readonly reports = input.required<ManagedReport[]>();
  readonly sortField = input.required<ReportSortField>();
  readonly sortDirection = input.required<SortDirection>();

  readonly openReport = output<ManagedReport>();
  readonly sortChange = output<ReportSortField>();
  readonly updateStatus = output<{ report: ManagedReport; status: ReportStatus }>();

  getShortId(id: string): string {
    return id.slice(0, 8);
  }

  getStatusTranslationKey(status: ReportStatus): string {
    return `MODERATOR.REPORTS.STATUS.${status}`;
  }

  getTargetTranslationKey(target: ReportTarget): string {
    return `MODERATOR.REPORTS.TARGET.${target}`;
  }

  getReasonTranslationKey(reason: ReportReason): string {
    return `MODERATOR.REPORTS.REASON.${reason}`;
  }

  getSortIndicator(field: ReportSortField): string {
    if (this.sortField() !== field) {
      return '↕';
    }

    return this.sortDirection() === 'asc' ? '↑' : '↓';
  }

  getReportPriority(reason: ReportReason): ReportPriority {
    switch (reason) {
      case 'SELF_HARM_OR_SUICIDE':
      case 'VIOLENCE_OR_THREATS':
      case 'HATE_SPEECH':
        return 'CRITICAL';

      case 'HARASSMENT':
      case 'PERSONAL_DATA':
      case 'FRAUD_OR_IMPERSONATION':
        return 'MEDIUM';

      default:
        return 'LOW';
    }
  }

  getPriorityBorderClass(report: ManagedReport): string {
    if (report.status === 'RESOLVED' || report.status === 'DISMISSED') {
      return 'border-l-transparent';
    }

    switch (this.getReportPriority(report.reason)) {
      case 'CRITICAL':
        return 'border-l-red-400';

      case 'MEDIUM':
        return 'border-l-amber-300';

      case 'LOW':
        return 'border-l-transparent';
    }
  }

  getTargetBadgeClasses(target: ReportTarget): string {
    switch (target) {
      case 'USER':
        return 'border-blue-300 bg-blue-50 text-blue-800';

      case 'PROJECT':
        return 'border-green-300 bg-green-50 text-green-800';

      case 'PROJECT_POST':
        return 'border-purple-300 bg-purple-50 text-purple-800';
    }
  }
  getTargetIcon(target: ReportTarget): string {
    switch (target) {
      case 'USER':
        return 'pi pi-user';

      case 'PROJECT':
        return 'pi pi-folder';

      case 'PROJECT_POST':
        return 'pi pi-comment';
    }
  }

  shouldShowSimilarReports(report: ManagedReport): boolean {
    return (
      report.similarReportsCount > 1 && (report.status === 'OPEN' || report.status === 'IN_REVIEW')
    );
  }
}
