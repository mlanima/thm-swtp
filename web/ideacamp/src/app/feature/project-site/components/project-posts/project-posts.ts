import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProjectPostResponse, ProjectResponse } from '../../../../models/project.model';
import { ProjectService } from '../../project.service';

@Component({
  selector: 'app-project-posts',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './project-posts.html',
})
export class ProjectPosts implements OnChanges {
  @Input({ required: true }) project!: ProjectResponse;
  @Input() canCreatePosts = false;

  private readonly projectService = inject(ProjectService);

  posts = signal<ProjectPostResponse[]>([]);
  isLoading = signal(false);
  isCreating = signal(false);
  errorMessage = signal<string | null>(null);

  visibleCount = signal(3);

  title = signal('');
  content = signal('');
  showCreateForm = signal(false);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['project'] && this.project?.id) {
      this.loadPosts();
    }
  }

  get visiblePosts(): ProjectPostResponse[] {
    return this.posts().slice(0, this.visibleCount());
  }

  get canLoadMore(): boolean {
    return this.visibleCount() < this.posts().length;
  }

  loadPosts(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.projectService.getProjectPosts(this.project.id).subscribe({
      next: (posts) => {
        this.posts.set(posts);
        this.visibleCount.set(3);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Posts konnten nicht geladen werden.');
        this.isLoading.set(false);
      },
    });
  }

  loadMore(): void {
    this.visibleCount.update((count) => count + 3);
  }

  toggleCreateForm(): void {
    this.showCreateForm.update((value) => !value);
    this.errorMessage.set(null);
  }

  createPost(): void {
    const title = this.title().trim();
    const content = this.content().trim();

    if (!title || !content) {
      this.errorMessage.set('Titel und Inhalt dürfen nicht leer sein.');
      return;
    }

    this.isCreating.set(true);
    this.errorMessage.set(null);

    this.projectService.createProjectPost(this.project.id, {
      title,
      content,
      contentFormat: 'PLAIN_TEXT',
      status: 'PUBLISHED',
    }).subscribe({
      next: (createdPost) => {
        this.posts.update((posts) => [createdPost, ...posts]);
        this.title.set('');
        this.content.set('');
        this.showCreateForm.set(false);
        this.isCreating.set(false);
      },
      error: () => {
        this.errorMessage.set('Post konnte nicht erstellt werden.');
        this.isCreating.set(false);
      },
    });
  }
}
