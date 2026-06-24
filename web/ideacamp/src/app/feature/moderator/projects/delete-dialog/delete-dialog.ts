import { Component, input, output, ChangeDetectionStrategy } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { DeleteState } from '../projects.types';

@Component({
  selector: 'app-delete-dialog',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './delete-dialog.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeleteDialog {
  readonly state = input.required<DeleteState>();
  readonly confirmInput = input('');
  readonly isDeleting = input(false);
  readonly isDeleteEnabled = input(false);
  readonly deleteError = input('');

  readonly closeDialog = output<void>();
  readonly confirm = output<void>();
  readonly confirmInputChange = output<string>();
}
