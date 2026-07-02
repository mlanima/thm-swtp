import { Component, signal, computed, inject } from '@angular/core';
import { NgClass } from '@angular/common';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ProjectSettingsService } from '../../services/project-settings.service';
import { ProjectSettingsStore } from '../../project-settings.store';
import { ProjectMemberResponse } from '../../models/project-settings.model';

@Component({
  selector: 'app-danger-zone-tab',
  standalone: true,
  imports: [NgClass, TranslatePipe],
  templateUrl: './danger-zone-tab.html',
})
export class DangerZoneTab {
  private readonly store = inject(ProjectSettingsStore);
  private readonly settingsService = inject(ProjectSettingsService);
  private readonly router = inject(Router);
  private readonly translateService = inject(TranslateService);

  // Delete
  showDeleteModal = signal(false);
  deleteConfirmInput = signal('');
  isDeleting = signal(false);
  deleteError = signal<string | null>(null);

  projectName = computed(() => this.store.project()?.name ?? '');
  projectId = computed(() => this.store.project()?.id ?? '');

  deleteEnabled = computed(() => this.deleteConfirmInput() === this.projectName());

  // Transfer
  showTransferModal = signal(false);
  members = signal<ProjectMemberResponse[]>([]);
  isLoadingMembers = signal(false);
  loadMembersError = signal<string | null>(null);
  selectedNewOwner = signal<ProjectMemberResponse | null>(null);
  isTransferring = signal(false);
  transferError = signal<string | null>(null);

  openDeleteModal(): void {
    this.deleteConfirmInput.set('');
    this.deleteError.set(null);
    this.showDeleteModal.set(true);
  }

  closeDeleteModal(): void {
    this.showDeleteModal.set(false);
    this.deleteConfirmInput.set('');
    this.deleteError.set(null);
  }

  confirmDelete(): void {
    if (!this.deleteEnabled() || this.isDeleting()) return;
    this.isDeleting.set(true);
    this.settingsService.deleteProject(this.projectId()).subscribe({
      next: () => this.router.navigateByUrl('/my-projects'),
      error: () => {
        this.deleteError.set(this.translateService.instant('PROJECTSETTINGS.DANGER.ERROR_DELETE_PROJECT'));
        this.isDeleting.set(false);
      },
    });
  }

  openTransferModal(): void {
    this.selectedNewOwner.set(null);
    this.transferError.set(null);
    this.showTransferModal.set(true);

    if (this.members().length > 0) return;

    const projectId = this.store.project()?.id;
    if (!projectId) return;

    this.isLoadingMembers.set(true);
    this.loadMembersError.set(null);

    this.settingsService.getProjectMembers(projectId).subscribe({
      next: (members) => {
        this.members.set(members);
        this.isLoadingMembers.set(false);
      },
      error: () => {
        this.loadMembersError.set(this.translateService.instant('PROJECTSETTINGS.DANGER.TRANSFER_ERROR_LOAD'));
        this.isLoadingMembers.set(false);
      },
    });
  }

  closeTransferModal(): void {
    if (this.isTransferring()) return;
    this.showTransferModal.set(false);
    this.selectedNewOwner.set(null);
    this.transferError.set(null);
  }

  selectNewOwner(member: ProjectMemberResponse): void {
    this.selectedNewOwner.set(member);
    this.transferError.set(null);
  }

  confirmTransfer(): void {
    const newOwner = this.selectedNewOwner();
    const projectId = this.store.project()?.id;
    if (!newOwner || !projectId || this.isTransferring()) return;

    this.isTransferring.set(true);
    this.transferError.set(null);

    this.settingsService.transferProjectOwnership(projectId, newOwner.keycloakId).subscribe({
      next: () => this.router.navigateByUrl('/my-projects'),
      error: () => {
        this.transferError.set(this.translateService.instant('PROJECTSETTINGS.DANGER.TRANSFER_ERROR'));
        this.isTransferring.set(false);
      },
    });
  }

  getInitials(name: string): string {
    return name.slice(0, 2).toUpperCase();
  }

  getAvatarColor(val: string): string {
    const colors = ['bg-lime-500', 'bg-slate-500', 'bg-rose-500', 'bg-blue-500', 'bg-violet-500'];
    return colors[val.length % colors.length];
  }
}
