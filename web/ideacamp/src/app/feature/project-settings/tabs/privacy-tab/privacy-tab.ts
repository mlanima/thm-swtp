import { Component, OnInit, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ProjectSettingsStore } from '../../project-settings.store';
import { ProjectService } from '../../../project-site/project.service';

@Component({
  selector: 'app-privacy-tab',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './privacy-tab.html',
})
export class PrivacyTab implements OnInit {
  private readonly store = inject(ProjectSettingsStore);
  private readonly projectService = inject(ProjectService);

  isPublic = signal(true);
  joinRequestsAllowed = signal(true);
  isSaving = signal(false);

  ngOnInit(): void {
    const project = this.store.project();
    if (project) {
      this.isPublic.set(!project.isPrivateProject);
      this.joinRequestsAllowed.set(project.allowJoinRequests);
    }
  }

  togglePublicVisibility(): void {
    const project = this.store.project();
    if (!project) return;

    const newValue = !this.isPublic();
    this.isPublic.set(newValue);
    this.isSaving.set(true);

    this.projectService
      .updateProject(project.id, {
        name: project.name,
        description: project.description,
        shortDescription: project.shortDescription ?? undefined,
        projectUrl: project.projectUrl,
        isPrivateProject: !newValue,
      })
      .subscribe({
        next: (response) => {
          this.isSaving.set(false);
          this.store.setProject(response);
        },
        error: () => {
          this.isPublic.set(!newValue);
          this.isSaving.set(false);
        },
      });
  }

  toggleAllowJoinRequests(): void {
    const project = this.store.project();
    if (!project) return;

    const newValue = !this.joinRequestsAllowed();
    this.joinRequestsAllowed.set(newValue);
    this.isSaving.set(true);

    this.projectService
      .updateAllowJoinRequests(project.id, newValue)
      .subscribe({
        next: (response) => {
          this.isSaving.set(false);
          this.store.setProject(response);
        },
        error: () => {
          this.joinRequestsAllowed.set(!newValue);
          this.isSaving.set(false);
        },
      });
  }
}
