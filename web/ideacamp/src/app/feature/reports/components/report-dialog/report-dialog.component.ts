import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { REPORT_REASONS, ReportReason, ReportTarget } from '../../models/report-create.model';

@Component({
  selector: 'app-report-dialog',
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  templateUrl: './report-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportDialogComponent {
  readonly target = input.required<ReportTarget>();
  readonly targetId = input.required<string>();
  readonly targetTitle = input<string | null>(null);
  readonly isSubmitting = input(false);
  readonly errorMessage = input<string | null>(null);

  readonly closeDialog = output<void>();
  readonly submitReport = output<{ reason: ReportReason; message?: string }>();

  readonly selectedReason = signal<ReportReason | null>(null);
  readonly message = signal('');

  readonly reasons = REPORT_REASONS;
  readonly maxMessageLength = 1000;

  submit(): void {
    const reason = this.selectedReason();

    if (!reason) {
      return;
    }

    const trimmedMessage = this.message().trim().slice(0, 1000);

    this.submitReport.emit({
      reason,
      message: trimmedMessage || undefined,
    });
  }
}
