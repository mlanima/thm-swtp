import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ManagedUser } from '../../models/managed-user.model';

@Component({
  selector: 'app-ban-user-dialog',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './ban-user-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BanUserDialogComponent {
  readonly user = input.required<ManagedUser>();
  readonly isSubmitting = input(false);

  readonly closeDialog = output<void>();
  readonly confirmBan = output<string | undefined>();

  readonly banReason = signal('');
  readonly maxBanReasonLength = 1000;

  getInitials(username: string): string {
    return username
      .split(/[.\s_-]+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join('');
  }

  submit(): void {
    const trimmedReason = this.banReason().trim().slice(0, this.maxBanReasonLength);

    this.confirmBan.emit(trimmedReason || undefined);
  }
}
