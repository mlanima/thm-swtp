import { ChangeDetectionStrategy, Component, input, output, effect } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { FormsModule } from '@angular/forms';
import { ManagedReport, ReportReason, ReportStatus, ReportTarget } from '../../models/report.model';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-report-detail-dialog',
  standalone: true,
  imports: [FormsModule, TranslatePipe, RouterLink],
  templateUrl: './report-detail-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportDetailDialogComponent {
  readonly report = input.required<ManagedReport>();
  readonly isUpdating = input(false);

  readonly closeDialog = output<void>();
  readonly updateStatus = output<{ status: ReportStatus; moderatorMessage?: string }>();

  moderatorMessage = '';

  constructor() {
    effect(() => {
      this.moderatorMessage = this.report().moderatorMessage ?? '';
    });
  }

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

  canSetInReview(report: ManagedReport): boolean {
    return report.status === 'OPEN';
  }

  canCloseReport(report: ManagedReport): boolean {
    return report.status === 'OPEN' || report.status === 'IN_REVIEW';
  }

  emitStatusUpdate(status: ReportStatus): void {
    this.updateStatus.emit({
      status,
      moderatorMessage: this.moderatorMessage.trim() || undefined,
    });
  }

  isClosed(report: ManagedReport): boolean {
    return report.status === 'RESOLVED' || report.status === 'DISMISSED';
  }
}
