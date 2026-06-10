import { Component, computed, Input, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProjectSettingsService } from '../../../project-settings/services/project-settings.service';
import { ProjectMemberResponse } from '../../../project-settings/models/project-settings.model';

interface MemberDisplay {
  id: string;
  name: string;
  initials: string;
  avatarColor: string;
  isOwner: boolean;
}

@Component({
  selector: 'app-member-list',
  standalone: true,
  imports: [NgClass, RouterLink],
  templateUrl: './member-list.html',
})
export class MemberList implements OnChanges {
  private readonly projectSettingsService = inject(ProjectSettingsService);

  @Input({ required: true }) projectId!: string;
  @Input() ownerId = '';
  @Input() ownerUsername = '';

  members = signal<MemberDisplay[]>([]);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  showModal = signal(false);

  openModal(): void { this.showModal.set(true); }
  closeModal(): void { this.showModal.set(false); }

  readonly hasMore = computed(() => this.members().length > 4);
  readonly visibleMembers = computed(() => this.hasMore() ? this.members().slice(0, 3) : this.members());
  readonly extraMembers = computed(() => this.members().slice(3));
  readonly extraCount = computed(() => this.members().length - 3);

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['projectId'] || changes['ownerId'] || changes['ownerUsername']) && this.projectId) {
      this.loadMembers();
    }
  }

  private loadMembers(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.projectSettingsService.getProjectMembers(this.projectId).subscribe({
      next: (members) => {
        const mapped = members.map((m) => this.toDisplay(m));
        if (this.ownerUsername && this.ownerId) {
          const ownerAlreadyPresent = mapped.some((m) => m.id === this.ownerId);
          if (!ownerAlreadyPresent) {
            mapped.unshift({
              id: this.ownerId,
              name: this.ownerUsername,
              initials: this.ownerUsername.slice(0, 2).toUpperCase(),
              avatarColor: this.getAvatarColor(this.ownerUsername),
              isOwner: true,
            });
          }
        }
        this.members.set(mapped);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Mitglieder konnten nicht geladen werden.');
        this.isLoading.set(false);
      },
    });
  }

  private toDisplay(member: ProjectMemberResponse): MemberDisplay {
    return {
      id: member.keycloakId,
      name: member.username,
      initials: member.username.slice(0, 2).toUpperCase(),
      avatarColor: this.getAvatarColor(member.username),
      isOwner: member.keycloakId === this.ownerId,
    };
  }

  private getAvatarColor(val: string): string {
    const colors = ['bg-lime-500', 'bg-slate-500', 'bg-rose-500', 'bg-blue-500', 'bg-violet-500'];
    return colors[val.length % colors.length];
  }
}
