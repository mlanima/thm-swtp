import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { ProjectResponse } from '../../../../models/project.model';
import { ProjectService } from '../../../project-site/project.service';

@Component({
  selector: 'app-privacy-tab',
  standalone: true,
  imports: [],
  templateUrl: './privacy-tab.html',
})
export class PrivacyTab implements OnInit {
  @Input() project!: ProjectResponse;

  private readonly projectService = inject(ProjectService);

  isPublic = signal(true);
  joinRequestsAllowed = signal(true);
  isSaving = signal(false);

  ngOnInit(): void {
    this.isPublic.set(!this.project.isPrivateProject);
    this.joinRequestsAllowed.set(this.project.allowJoinRequests);
  }

  togglePublicVisibility(): void {
    const newValue = !this.isPublic();
    this.isPublic.set(newValue);
    this.isSaving.set(true);

    this.projectService
      .updateProject(this.project.id, {
        name: this.project.name,
        description: this.project.description,
        shortDescription: this.project.shortDescription ?? undefined,
        projectUrl: this.project.projectUrl,
        isPrivateProject: !newValue,
      })
      .subscribe({
        next: () => this.isSaving.set(false),
        error: () => {
          this.isPublic.set(!newValue);
          this.isSaving.set(false);
        },
      });
  }

  toggleAllowJoinRequests(): void {
    const newValue = !this.joinRequestsAllowed();
    this.joinRequestsAllowed.set(newValue);
    this.isSaving.set(true);

    this.projectService
      .updateAllowJoinRequests(this.project.id, newValue)
      .subscribe({
        next: () => this.isSaving.set(false),
        error: () => {
          this.joinRequestsAllowed.set(!newValue);
          this.isSaving.set(false);
        },
      });
  }
}
