import { Component, OnInit, inject, signal, DestroyRef, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MyProjectsService } from '../../services/my-projects.service';
import { ProjectResponse } from '../../../../models/project.model';
import { AuthService } from '../../../auth/auth.service';
import { SearchService } from '../../../search/services/search.service';
import { ProjectFilter } from '../../components/project-filter/project-filter';
import { ProjectList } from '../../components/project-list/project-list';
import { InvitationsSection } from '../../components/invitations-section/invitations-section';

@Component({
  selector: 'app-my-projects-page',
  standalone: true,
  imports: [RouterLink, ProjectFilter, ProjectList, InvitationsSection],
  templateUrl: './my-projects-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyProjectsPage implements OnInit {
  private readonly myProjectsService = inject(MyProjectsService);
  private readonly authService = inject(AuthService);
  private readonly searchService = inject(SearchService);
  private readonly destroyRef = inject(DestroyRef);

  readonly projects = signal<ProjectResponse[]>([]);
  readonly isLoading = signal(true);
  readonly isFiltering = signal(false);
  readonly errorMessage = signal('');
  readonly projectTags = signal<Map<string, string[]>>(new Map());
  readonly activeFilter = signal<'my' | 'all'>('my');

  ngOnInit(): void {
    this.authService.waitUntilAuthReady().then(() => this.loadProjects());
  }

  setFilter(filter: 'my' | 'all'): void {
    if (this.activeFilter() === filter) return;
    this.activeFilter.set(filter);
    this.projects.set([]);
    this.isFiltering.set(true);
    this.errorMessage.set('');
    setTimeout(() => this.loadProjects(true));
  }

  private loadProjects(isFilterSwitch = false): void {
    const username = this.authService.username();

    if (!username) {
      this.errorMessage.set('Dein Benutzername konnte nicht geladen werden.');
      this.isLoading.set(false);
      this.isFiltering.set(false);
      return;
    }

    if (!isFilterSwitch) {
      this.isLoading.set(true);
    }
    this.errorMessage.set('');

    const onNext = (projects: ProjectResponse[]) => {
      this.projects.set(projects);
      this.isLoading.set(false);
      this.isFiltering.set(false);
      this.loadAllTags(projects);
    };

    const onError = () => {
      this.errorMessage.set('Deine Projekte konnten nicht geladen werden.');
      this.isLoading.set(false);
      this.isFiltering.set(false);
    };

    const request$ =
      this.activeFilter() === 'all'
        ? this.myProjectsService.getAllProjects(username)
        : this.myProjectsService.getMyProjects(username);

    request$.subscribe({ next: onNext, error: onError });
  }

  private loadAllTags(projects: ProjectResponse[]): void {
    const current = this.projectTags();
    for (const project of projects) {
      if (current.has(project.id)) {
        continue;
      }
      const sub = this.searchService.getProjectTags(project.id).subscribe({
        next: (tags) => {
          this.projectTags.update((map) => {
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
