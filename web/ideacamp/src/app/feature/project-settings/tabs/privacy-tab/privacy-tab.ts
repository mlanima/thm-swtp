import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProjectResponse } from '../../../../models/project.model';
import { ProjectService } from '../../../project-site/project.service';

@Component({
  selector: 'app-privacy-tab',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './privacy-tab.html',
})
export class PrivacyTab implements OnInit {
  @Input() project!: ProjectResponse;

  private readonly projectService = inject(ProjectService);

  isPublic = signal(true);
  joinRequestsAllowed = signal(true);
  isSaving = signal(false);
  saveError = signal<string | null>(null);

  ngOnInit(): void {
    this.isPublic.set(!this.project.isPrivateProject);
    this.joinRequestsAllowed.set(this.project.allowJoinRequests);
  }

  togglePublicVisibility(): void {
    const newIsPublic = !this.isPublic();
    this.isSaving.set(true);
    this.saveError.set(null);

    this.projectService.updateProject(this.project.id, {
      name: this.project.name,
      description: this.project.description,
      projectUrl: this.project.projectUrl,
      isPrivateProject: !newIsPublic,
    }).subscribe({
      next: () => {
        this.isPublic.set(newIsPublic);
        this.isSaving.set(false);
      },
      error: () => {
        this.saveError.set('Einstellung konnte nicht gespeichert werden.');
        this.isSaving.set(false);
      },
    });
  }

  toggleAllowJoinRequests(): void {
    const newValue = !this.joinRequestsAllowed();
    this.joinRequestsAllowed.set(newValue);
    this.projectService.updateAllowJoinRequests(this.project.id, newValue).subscribe({
      error: () => this.joinRequestsAllowed.set(!newValue),
    });
  }
}
