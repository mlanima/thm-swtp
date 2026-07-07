import { Component, input, signal, effect, inject, ChangeDetectionStrategy } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ProjectResponse, ProjectPostResponse } from '../../../../models/project.model';
import { ProjectService } from '../../../project-site/project.service';
import { MarkdownPipe } from '../../../../shared/pipes/markdown.pipe';
import { getInitials, getAvatarColor } from '../../../../shared/utils/avatar.util';
import { timeAgo } from '../../../../shared/utils/relative-time.util';

interface FeedPost extends ProjectPostResponse {
  projectName: string;
  projectUrl: string;
}

const MAX_POSTS = 15;

@Component({
  selector: 'app-recent-posts',
  standalone: true,
  imports: [TranslatePipe, MarkdownPipe],
  templateUrl: './recent-posts.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    .collapsible-content {
      max-height: 0;
      overflow: hidden;
      transition: max-height 0.35s cubic-bezier(0.22, 1, 0.36, 1);
    }
    .collapsible-content.expanded {
      max-height: 5000px;
    }
  `],
})
export class RecentPosts {
  readonly projects = input.required<ProjectResponse[]>();

  private readonly projectService = inject(ProjectService);
  private readonly translateService = inject(TranslateService);

  readonly posts = signal<FeedPost[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly expanded = signal(true);

  toggle(): void {
    this.expanded.update((v) => !v);
  }

  constructor() {
    effect(() => {
      const projects = this.projects();
      if (projects.length === 0) {
        this.posts.set([]);
        return;
      }
      this.loadPosts(projects);
    });
  }

  getInitials(name: string): string {
    return getInitials(name);
  }

  getAvatarColor(name: string): string {
    return getAvatarColor(name);
  }

  timeAgo(dateStr: string): string {
    return timeAgo(dateStr, this.translateService);
  }

  private loadPosts(projects: ProjectResponse[]): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    const requests = projects.map((project) =>
      this.projectService.getProjectPosts(project.id).pipe(
        catchError(() => of<ProjectPostResponse[]>([])),
      ),
    );

    forkJoin(requests).subscribe({
      next: (results) => {
        const merged: FeedPost[] = results.flatMap((posts, index) =>
          posts.map((post) => ({
            ...post,
            projectName: projects[index].name,
            projectUrl: projects[index].projectUrl,
          })),
        );
        merged.sort((a, b) => {
          const aTime = new Date(a.publishedAt ?? a.createdAt).getTime();
          const bTime = new Date(b.publishedAt ?? b.createdAt).getTime();
          return bTime - aTime;
        });
        this.posts.set(merged.slice(0, MAX_POSTS));
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translateService.instant('DASHBOARD.POSTS.ERROR_LOAD'));
        this.isLoading.set(false);
      },
    });
  }
}
