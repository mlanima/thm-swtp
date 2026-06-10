import { Component, Input, signal, OnChanges, SimpleChanges, inject, computed } from '@angular/core';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProjectMember, ProjectMemberResponse } from '../../models/project-settings.model';
import { ProjectSettingsService } from '../../services/project-settings.service';
import { UserSearchPick } from '../../../../shared/user-search-pick/user-search-pick';
import { UserSearchResult } from '../../../search/models/user-search-result.model';

@Component({
  selector: 'app-members-tab',
  standalone: true,
  imports: [NgClass, FormsModule, UserSearchPick],
  templateUrl: './members-tab.html',
})
export class MembersTab implements OnChanges {
  private readonly projectSettingsService = inject(ProjectSettingsService);
  @Input() projectId = '';
  @Input() ownerId = '';




  members = signal<ProjectMember[]>([]);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  isDeleting = signal(false);

  memberToRemove = signal<ProjectMember | null>(null);

  isInviteModalOpen = signal(false);
  selectedUserToInvite = signal<UserSearchResult | null>(null);
  inviteMessage = signal('');
  isInviting = signal(false);
  inviteErrorMessage = signal<string | null>(null);
  inviteSuccessMessage = signal<string | null>(null);

  pendingInvitedUserIds = signal<string[]>([]);

  excludedUserIds = computed(() => [this.ownerId,
    ... this.members().map((mem) => mem.id),
    ... this.pendingInvitedUserIds()]);




  ngOnChanges(changes: SimpleChanges): void {
    if (changes['projectId'] && this.projectId) {
      this.loadMembers();
      this.loadUserIdPendingInvites();
    }
  }

  openRemoveModal(member: ProjectMember): void {
    this.memberToRemove.set(member);
  }

  closeRemoveModal(): void {
    this.memberToRemove.set(null);
  }

  confirmRemove(): void {
    const member = this.memberToRemove();
    if (!member || !this.projectId || member.role === 'Owner') return;

    this.isDeleting.set(true);
    this.errorMessage.set(null);

    this.projectSettingsService.deleteProjectMember(this.projectId,member.id).subscribe({
      next: () => {
        this.members.update((list) => list.filter((mem) => mem.id !== member.id));
        this.memberToRemove.set(null);
        this.isDeleting.set(false);
      },
      error: () => {
        this.errorMessage.set('Mitglied konnte nicht gelöscht werden. Bitte versuchen Sie es gleich nochmal.');
        this.isDeleting.set(false);
      },
    });
  }

  openInviteModal(): void {
    this.isInviteModalOpen.set(true);
    this.inviteMessage.set('');
    this.inviteErrorMessage.set(null);
    this.inviteSuccessMessage.set(null);
    this.selectedUserToInvite.set(null);
  }

  closeInviteModal(): void {
    if (this.isInviting()) {
      return;
    }
    this.isInviteModalOpen.set(false);
    this.inviteMessage.set('');
    this.inviteErrorMessage.set(null);
    this.selectedUserToInvite.set(null);
  }

  selectUserToInvite(user : UserSearchResult): void {
    this.selectedUserToInvite.set(user);
  }

  inviteSelectedUser(): void {
    const user = this.selectedUserToInvite();

    if (!user || !this.projectId || this.isInviting()) {
      return;
    }

    this.isInviting.set(true);
    this.inviteErrorMessage.set(null);

    this.projectSettingsService.createProjectInvite(this.projectId,{
      invitedUserId: user.keycloakId,
      message: this.inviteMessage().trim() || undefined,
    })
      .subscribe({
        next: () => {
          this.inviteSuccessMessage.set(`Einladung wurde erfolgreich an ${user.username} gesendet.`);
          this.pendingInvitedUserIds.update((ids) => [...ids, user.keycloakId]);
          this.isInviting.set(false);
          this.closeInviteModal();

          setTimeout(() => {
            this.inviteSuccessMessage.set(null)
          }, 5000);
        },
        error : () => {
          this.inviteErrorMessage.set('Einladung konnte nicht erstellt werden, bitte versuchen Sie es erneut.');
          this.isInviting.set(false);
        },
      });
  }



  private loadMembers(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.projectSettingsService.getProjectMembers(this.projectId).subscribe({
      next: (members) => {
        this.members.set(members.map((member) => this.toProjectMember(member)));
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Mitglieder konnten nicht geladen werden. Bitte versuchen sie es später nochmal.');
        this.isLoading.set(false);
      },
    });
  }

  private toProjectMember(member: ProjectMemberResponse): ProjectMember {
    return {
      id: member.keycloakId,
      name: member.username,
      email: member.email,
      initials: this.getInitials(member.username),
      avatarColor: this.getAvatarColor(member.username),
      role: 'Member',
    }
  }

  private getInitials(name : string): string {
    return name.slice(0, 2).toUpperCase();
  }

  private getAvatarColor(val: string): string {
    const colors = ['bg-lime-500', 'bg-slate-500', 'bg-rose-500', 'bg-blue-500', 'bg-violet-500'];
    const index = val.length % colors.length;
    return colors[index];
  }

  private loadUserIdPendingInvites(): void {
    this.projectSettingsService.getProjectInvites(this.projectId).subscribe({
      next: (invites) => {
        this.pendingInvitedUserIds.set(
          invites
            .filter((invite) => invite.status === 'PENDING')
            .map((invite) => invite.invitedUserId),
        );
      },
      error: () => {
        this.pendingInvitedUserIds.set([]);
      }
    });
  }
}
