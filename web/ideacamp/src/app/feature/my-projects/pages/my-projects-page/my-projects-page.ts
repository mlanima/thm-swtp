import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MyProjectsService } from '../../services/my-projects.service';
import { ProjectResponse } from '../../../../models/project.model';
import { AuthService } from '../../../auth/auth.service';
import { FavoriteButton } from '../../../../shared/favorite-button/favorite-button';
import { SearchService } from '../../../search/services/search.service';
import { DestroyRef } from '@angular/core';

@Component({
  selector: 'app-my-projects-page',
  standalone: true,
  imports: [RouterLink, FavoriteButton],
  templateUrl: './my-projects-page.html',
})
export class MyProjectsPage implements OnInit {
  private readonly myProjectsService = inject(MyProjectsService);
  private readonly authService = inject(AuthService);
  private readonly searchService = inject(SearchService);
  private readonly destroyRef = inject(DestroyRef);

  readonly projects = signal<ProjectResponse[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal('');
  readonly projectTags = signal<Map<string, string[]>>(new Map());
  readonly invitationsExpanded = signal(false);

  async ngOnInit(): Promise<void> {
    await this.authService.waitUntilAuthReady();
    this.loadProjects();
  }

  getInitials(name: string): string {
    return name
      .split(' ')
      .slice(0, 2)
      .map(w => w[0]?.toUpperCase() ?? '')
      .join('');
  }

  getAvatarColor(name: string): string {
    const colors = [
      'bg-purple-500', 'bg-blue-500', 'bg-green-500', 'bg-amber-500',
      'bg-rose-500', 'bg-cyan-500', 'bg-indigo-500', 'bg-teal-500',
    ];
    let hash = 0;
    for (const char of name) {
      hash = char.charCodeAt(0) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }

  getTagsForProject(projectId: string): string[] {
    return this.projectTags().get(projectId) ?? [];
  }

  toggleInvitations(): void {
    this.invitationsExpanded.update(v => !v);
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
        this.loadAllTags(projects);
      },
      error: () => {
        this.errorMessage.set('Deine Projekte konnten nicht geladen werden.');
        this.isLoading.set(false);
      },
    });
  }

  private loadAllTags(projects: ProjectResponse[]): void {
    for (const project of projects) {
      const sub = this.searchService.getProjectTags(project.id).subscribe({
        next: tags => {
          this.projectTags.update(map => {
            const newMap = new Map(map);
            newMap.set(project.id, tags);
            return newMap;
          });
        },
      });
      this.destroyRef.onDestroy(() => sub.unsubscribe());
    }
  }
}



