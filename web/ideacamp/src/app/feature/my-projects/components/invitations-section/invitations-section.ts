import { Component, signal, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-invitations-section',
  standalone: true,
  templateUrl: './invitations-section.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvitationsSection {
  readonly invitationsExpanded = signal(false);

  toggle(): void {
    this.invitationsExpanded.update((v) => !v);
  }
}
