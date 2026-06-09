import { Component, Input, signal, OnChanges, SimpleChanges, inject } from '@angular/core';
import { NgClass } from '@angular/common';
import { ProjectMember, ProjectMemberResponse } from '../../models/project-settings.model';
import { ProjectSettingsService } from '../../services/project-settings.service';

@Component({
  selector: 'app-members-tab',
  standalone: true,
  imports: [NgClass],
  templateUrl: './members-tab.html',
})
export class MembersTab implements OnChanges {
  private readonly projectSettingsService = inject(ProjectSettingsService);
  @Input() projectId = '';

  members = signal<ProjectMember[]>([]);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  isDeleting = signal(false);

  memberToRemove = signal<ProjectMember | null>(null);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['projectId'] && this.projectId) {
      this.loadMembers();
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
        this.errorMessage.set('Could not delete member. Please try again later.');
        this.isDeleting.set(false);
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
        this.errorMessage.set('Could not load members. Please try again later.');
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
}
