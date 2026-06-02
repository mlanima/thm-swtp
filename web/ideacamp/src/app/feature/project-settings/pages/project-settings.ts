import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

type SettingsTab = 'join-requests' | 'members' | 'privacy' | 'danger-zone';

interface JoinRequest {
  id: string;
  name: string;
  email: string;
  initials: string;
  message?: string;
  date: string;
  colorClass: string;
}

interface ProjectMember {
  id: string;
  name: string;
  email: string;
  initials: string;
  role: 'Owner' | 'Member';
  joined: string;
}

@Component({
  selector: 'app-project-settings',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './project-settings.html',
})
export class ProjectSettings {
  private readonly route = inject(ActivatedRoute);
  protected readonly projectUrl = this.route.snapshot.paramMap.get('projectUrl') ?? '';

  protected readonly activeTab = signal<SettingsTab>('join-requests');

  protected readonly publicVisibility = signal(true);
  protected readonly allowJoinRequests = signal(false);

  protected readonly selectedMemberToRemove = signal<ProjectMember | null>(null);

  protected readonly showDeleteProjectDialog = signal(false);
  protected readonly deleteProjectConfirmation = signal('');

  protected readonly projectName = signal('IdeaCamp');
  protected readonly deleteConfirmationText = computed(
    () => `${this.projectName()} Project`,
  );

  protected readonly isDeleteProjectConfirmationValid = computed(
    () =>
      this.deleteProjectConfirmation().trim() ===
      this.deleteConfirmationText(),
  );

  protected readonly joinRequests = signal<JoinRequest[]>([
    {
      id: '6f2b3e4a-3c1d-4b7a-9c8e-1a2b3c4d5e6f',
      name: 'Maria Schmidt',
      email: 'm.schmidt@example.com',
      initials: 'MS',
      message: 'I would love to contribute my UX expertise.',
      date: 'May 29, 2026',
      colorClass: 'bg-lime-500',
    },
    {
      id: '1c8e9a2b-4d5f-4a3b-8c7d-9e0f1a2b3c4d',
      name: 'Luca Bianchi',
      email: 'luca.b@example.com',
      initials: 'LB',
      message: 'Interested in helping with the roadmap planning.',
      date: 'May 28, 2026',
      colorClass: 'bg-slate-700',
    },
    {
      id: '9a7b6c5d-4e3f-4a2b-8c1d-0e9f8a7b6c5d',
      name: 'Sophie Müller',
      email: 'sophie.m@example.com',
      initials: 'SM',
      date: 'May 27, 2026',
      colorClass: 'bg-lime-500',
    },
  ]);

  protected readonly members = signal<ProjectMember[]>([
    {
      id: '2d4f6a8b-1c3e-4f5a-9b7d-8e6c4a2f0b1d',
      name: 'Example User',
      email: 'example@thm.de',
      initials: 'EU',
      role: 'Owner',
      joined: 'Jan 1, 2026',
    },
    {
      id: '7b9d1f3a-5c6e-4d8a-9f2b-1e3c5a7d9b0f',
      name: 'Anna Hofmann',
      email: 'a.hofmann@thm.de',
      initials: 'AH',
      role: 'Member',
      joined: 'Feb 12, 2026',
    },
    {
      id: '4a6c8e0f-2b3d-4f5a-8c9e-1d2f3a4b5c6e',
      name: 'Felix Wagner',
      email: 'f.wagner@thm.de',
      initials: 'FW',
      role: 'Member',
      joined: 'Mar 3, 2026',
    },
    {
      id: '8e6c4a2f-0b1d-4f3a-9c7e-5d2b1a0f9e8c',
      name: 'Carla Rossi',
      email: 'c.rossi@thm.de',
      initials: 'CR',
      role: 'Member',
      joined: 'Apr 20, 2026',
    },
  ]);

  protected readonly hasJoinRequests = computed(
    () => this.joinRequests().length > 0,
  );

  /**
   * Changes the currently visible settings tab
   */
  protected selectTab(tab: SettingsTab): void {
    this.activeTab.set(tab);
  }

  /**
   * Approves a join request and removes it from the pending request list
   */
  protected approveJoinRequest(requestId: string): void {
    this.joinRequests.update((requests) =>
      requests.filter((request) => request.id !== requestId),
    );
    //backend-call later
  }

  /**
   * Declines a join request and removes it from the pending request list
   */
  protected declineJoinRequest(requestId: string): void {
    this.joinRequests.update((requests) =>
      requests.filter((request) => request.id !== requestId),
    );
    //backend-call later
  }

  /**
   * Opens the confirmation dialog for removing a project member
   */
  protected openRemoveMemberDialog(member: ProjectMember): void {
    this.selectedMemberToRemove.set(member);
  }

  /**
   * Closes the remove member confirmation dialog
   */
  protected closeRemoveMemberDialog(): void {
    this.selectedMemberToRemove.set(null);
  }

  /**
   * Removes the selected member from the members list
   */
  protected confirmRemoveMember(): void {
    const member = this.selectedMemberToRemove();

    if (!member) {
      return;
    }

    this.members.update((members) =>
      members.filter((currentMember) => currentMember.id !== member.id),
    );

    this.closeRemoveMemberDialog();
    //backend-call later
  }

  /**
   * Toggles whether the project is publicly visible
   */
  protected togglePublicVisibility(): void {
    this.publicVisibility.update((value) => !value);
    //backend-call later
  }

  /**
   * Toggles whether users can request to join the project
   */
  protected toggleJoinRequests(): void {
    this.allowJoinRequests.update((value) => !value);
    //backend-call later
  }

  /**
   * Opens the delete project confirmation dialog
   */
  protected openDeleteProjectDialog(): void {
    this.deleteProjectConfirmation.set('');
    this.showDeleteProjectDialog.set(true);
  }

  /**
   * Closes the delete project confirmation dialog
   */
  protected closeDeleteProjectDialog(): void {
    this.deleteProjectConfirmation.set('');
    this.showDeleteProjectDialog.set(false);
  }

  /**
   * Updates the delete project confirmation input value
   */
  protected updateDeleteProjectConfirmation(value: string): void {
    this.deleteProjectConfirmation.set(value);
  }

  /**
   * Deletes the project after the confirmation text has been entered correctly
   */
  protected confirmDeleteProject(): void {
    if (!this.isDeleteProjectConfirmationValid()) {
      return;
    }

    this.closeDeleteProjectDialog();
  }
  //backend-call later
}
