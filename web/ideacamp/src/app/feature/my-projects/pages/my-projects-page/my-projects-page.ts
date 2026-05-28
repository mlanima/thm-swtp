import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MyProjectsService } from '../../services/my-projects.service';
import { ProjectResponse } from '../../../../models/project.model';
import { AuthService } from '../../../auth/auth.service';

@Component({
  selector: 'app-my-projects-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './my-projects-page.html',
})
export class MyProjectsPage implements OnInit {
  private readonly myProjectsService = inject(MyProjectsService);
  private readonly authService = inject(AuthService);

  readonly projects = signal<ProjectResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal('');

  async ngOnInit(): Promise<void> {
    await this.authService.waitUntilAuthReady();
    this.loadProjects();
  }

  private loadProjects(): void {
    const username = this.authService.username();

    if (!username) {
      this.errorMessage.set('Dein Benutzername konnte nicht geladen werden.');
      this.isLoading.set(false);
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    this.myProjectsService.getMyProjects(username).subscribe({
      next: projects => {
        this.projects.set(projects);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Deine Projekte konnten nicht geladen werden.');
        this.isLoading.set(false);
      },
    });
  }
}



