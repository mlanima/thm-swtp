import { Component, inject, OnInit, signal } from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import{CommonModule} from '@angular/common';
import{FormsModule} from '@angular/forms';
import {ProjectService} from './project.service';
import {ProjectResponse } from '../../models/project.model';
import { ProjectHeader } from './components/project-header/project-header';
import { InfoCard } from './components/info-card/info-card';
import { ProjectSidebar } from './components/project-sidebar/project-sidebar';
import {AuthService} from '../auth/auth.service';


@Component({
  selector: 'app-project-site',
  standalone: true,
  imports: [ProjectHeader, InfoCard, ProjectSidebar, FormsModule, CommonModule, RouterLink],
  templateUrl: './project-site.html',
})
export class ProjectSite  implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly projectService = inject(ProjectService);
  private readonly authService = inject(AuthService);

  project = signal<ProjectResponse | null>(null);
  errorMessage = signal<string | null>(null);
  isLoading = signal(true);

  isEditing = signal(false);
  isSaving = signal(false);
  successMessage = signal<string | null>(null);
  editName = signal('');
  editDescription = signal('');

  get isOwner(): boolean {
    const user = this.authService.user();
    const proj = this.project();
    if (!user || !proj) return false;
    return user.id === proj.ownerId;
  }

  ngOnInit(): void {
    const projectUrl = this.route.snapshot.paramMap.get('projectUrl');
    if (!projectUrl) {
      this.errorMessage.set('Keine Projekt-URL angegeben.');
      this.isLoading.set(false);
      return;
    }

    this.projectService.getProjectByUrl(projectUrl).subscribe({
      next: (data) => {
        this.project.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Projektdaten konnten nicht abgerufen werden.');
        this.isLoading.set(false);
      },
    });
  }

  startEdit(): void {
    const proj = this.project();
    if (!proj) return;
    this.editName.set(proj.name);
    this.editDescription.set(proj.description);
    this.isEditing.set(true);
    this.successMessage.set(null);
  }

  cancelEdit(): void {
    this.isEditing.set(false);
  }

  saveEdit(): void {
    const proj = this.project();
    if (!proj) return;
    this.isSaving.set(true);

    this.projectService
      .updateProject(proj.id, {
        name: this.editName(),
        description: this.editDescription(),
        projectUrl: proj.projectUrl,
        isPrivateProject: proj.isPrivateProject,
      })
      .subscribe({
        next: (updated) => {
          this.project.set(updated);
          this.isEditing.set(false);
          this.isSaving.set(false);
          this.successMessage.set('Projekt erfolgreich gespeichert!');
          setTimeout(() => this.successMessage.set(null), 3000);
        },
        error: () => {
          this.errorMessage.set('Projekt konnte nicht gespeichert werden.');
          this.isSaving.set(false);
        },
      });
  }
}
