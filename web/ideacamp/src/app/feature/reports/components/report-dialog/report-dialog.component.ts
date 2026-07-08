import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { REPORT_REASONS, ReportReason, ReportTarget } from '../../models/report-create.model';
import { createReportSchema } from '../../schemas/report-create.schema';

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
    const result = createReportSchema.safeParse({
      target: this.target(),
      targetId: this.targetId(),
      reason: this.selectedReason(),
      message: this.message(),
    });

    if (!result.success) {
      return;
    }

    this.submitReport.emit({
      reason: result.data.reason,
      message: result.data.message?.trim() || undefined,
    });
  }
}
