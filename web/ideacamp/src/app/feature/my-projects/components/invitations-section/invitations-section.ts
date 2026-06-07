import { Component, OnInit, signal, inject, ChangeDetectionStrategy } from '@angular/core';
import { ProjectInvitationService } from '../../services/project-invitation.service';
import { ProjectInviteResponse } from '../../../../models/project-invite.model';
import { InvitationCard } from '../invitation-card/invitation-card';

@Component({
  selector: 'app-invitations-section',
  standalone: true,
  imports: [InvitationCard],
  templateUrl: './invitations-section.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvitationsSection implements OnInit {
  private readonly invitationService = inject(ProjectInvitationService);

  readonly invitations = signal<ProjectInviteResponse[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly invitationsExpanded = signal(false);

  ngOnInit(): void {
    this.loadInvitations();
  }

  toggle(): void {
    this.invitationsExpanded.update((v) => !v);
  }

  accept(invite: ProjectInviteResponse): void {
    this.invitationService.updateInvitationStatus(invite.id, 'ACCEPTED').subscribe({
      next: () => {
        this.invitations.update((list) => list.filter((i) => i.id !== invite.id));
      },
    });
  }

  decline(invite: ProjectInviteResponse): void {
    this.invitationService.updateInvitationStatus(invite.id, 'REJECTED').subscribe({
      next: () => {
        this.invitations.update((list) => list.filter((i) => i.id !== invite.id));
      },
    });
  }

  private loadInvitations(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.invitationService.getInvitations().subscribe({
      next: (invitations) => {
        this.invitations.set(invitations.filter((i) => i.status === 'PENDING'));
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Invitations could not be loaded.');
        this.isLoading.set(false);
      },
    });
  }
}
