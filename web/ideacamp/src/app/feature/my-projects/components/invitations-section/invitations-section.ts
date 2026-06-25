import { Component, OnInit, signal, inject, ChangeDetectionStrategy } from '@angular/core';
import { ProjectInvitationService } from '../../services/project-invitation.service';
import { ProjectInviteResponse } from '../../../../models/project-invite.model';
import { InvitationCard } from '../invitation-card/invitation-card';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { SidebarService } from '../../../../shared/sidebar/sidebar.service';

@Component({
  selector: 'app-invitations-section',
  standalone: true,
  imports: [InvitationCard, TranslatePipe],
  templateUrl: './invitations-section.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    .collapsible-content {
      max-height: 0;
      overflow: hidden;
      transition: max-height 0.35s cubic-bezier(0.22, 1, 0.36, 1);
    }
    .collapsible-content.expanded {
      max-height: 999px;
    }
    .invitation-item {
      opacity: 0;
      transform: translateY(-8px);
      transition: opacity 0.3s ease-out, transform 0.3s ease-out;
    }
    .collapsible-content.expanded .invitation-item {
      opacity: 1;
      transform: translateY(0);
    }
  `],
})
export class InvitationsSection implements OnInit {
  private readonly invitationService = inject(ProjectInvitationService);
  private readonly translateService = inject(TranslateService);
  private readonly sidebarService = inject(SidebarService);
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
        this.sidebarService.pendingInvitationsCount.set(this.invitations().length);
      },
    });
  }

  decline(invite: ProjectInviteResponse): void {
    this.invitationService.updateInvitationStatus(invite.id, 'REJECTED').subscribe({
      next: () => {
        this.invitations.update((list) => list.filter((i) => i.id !== invite.id));
        this.sidebarService.pendingInvitationsCount.set(this.invitations().length);
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
        this.errorMessage.set(this.translateService.instant('MYPROJECTS.ERROR_LOAD_INVITATIONS'));
        this.isLoading.set(false);
      },
    });
  }
}
