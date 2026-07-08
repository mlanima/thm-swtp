import { Component, input, signal, effect, inject, DestroyRef, ChangeDetectionStrategy } from '@angular/core';
import { forkJoin, of, Subscription } from 'rxjs';
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
  private readonly destroyRef = inject(DestroyRef);

  private postsSubscription?: Subscription;


  readonly posts = signal<FeedPost[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly expanded = signal(true);
  readonly postImageUrls = signal<Record<string, string>>({});

  toggle(): void {
    this.expanded.update((v) => !v);
  }

  constructor() {
    effect(() => {
      const projects = this.projects();
      if (projects.length === 0) {
        this.posts.set([]);
        this.postsSubscription?.unsubscribe();
        this.clearPostImageUrls();
        return;
      }
      this.loadPosts(projects);
    });
    this.destroyRef.onDestroy(() => {
      this.postsSubscription?.unsubscribe();
      this.clearPostImageUrls();
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
    this.postsSubscription?.unsubscribe();
    this.isLoading.set(true);
    this.errorMessage.set('');

    const requests = projects.map((project) =>
      this.projectService.getProjectPosts(project.id).pipe(
        catchError(() => of<ProjectPostResponse[]>([])),
      ),
    );


    this.postsSubscription = forkJoin(requests).subscribe({
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

        const visiblePosts = merged.slice(0, MAX_POSTS);
        this.posts.set(visiblePosts);
        this.isLoading.set(false);
        this.clearPostImageUrls();
        this.loadPostImages(visiblePosts);
      },
      error: () => {
        this.errorMessage.set(this.translateService.instant('DASHBOARD.POSTS.ERROR_LOAD'));
        this.isLoading.set(false);
      },
    });
  }

  private loadPostImages(posts: FeedPost[]): void {
    posts
      .filter((post) => !!post.imageUrl)
      .forEach((post) => {
        this.projectService.getProjectPostImage(post.projectId, post.id).subscribe({
          next: (blob: Blob) => {
            const objectUrl = URL.createObjectURL(blob);
            this.postImageUrls.update((urls) => ({ ...urls, [post.id]: objectUrl }));
          },
        });
      });
  }

  private clearPostImageUrls(): void {
    Object.values(this.postImageUrls()).forEach((url) => URL.revokeObjectURL(url));
    this.postImageUrls.set({});
  }
}
