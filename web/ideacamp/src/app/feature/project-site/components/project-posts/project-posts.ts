import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges, inject, signal, ElementRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProjectPostResponse, ProjectResponse } from '../../../../models/project.model';
import { ProjectService } from '../../project.service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { MarkdownPipe } from '../../../../shared/pipes/markdown.pipe';

type ProjectPostContentFormat = 'PLAIN_TEXT' | 'MARKDOWN';

@Component({
  selector: 'app-project-posts',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe, MarkdownPipe],
  templateUrl: './project-posts.html',
})

export class ProjectPosts implements OnChanges {
  @Input({ required: true }) project!: ProjectResponse;
  @Input() canCreatePosts = false;

  private readonly projectService = inject(ProjectService);
  private readonly translateService = inject(TranslateService);

  posts = signal<ProjectPostResponse[]>([]);
  isLoading = signal(false);
  isCreating = signal(false);
  errorMessage = signal<string | null>(null);

  visibleCount = signal(3);

  title = signal('');
  content = signal('');
  showCreateForm = signal(false);

  contentFormat = signal<ProjectPostContentFormat>('MARKDOWN');

  @ViewChild('postContentInput')
  postContentInput?: ElementRef<HTMLTextAreaElement>;

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
        this.errorMessage.set(this.translateService.instant('PROJECTPOSTS.ERRORS.LOAD'));
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
      this.errorMessage.set(this.translateService.instant('PROJECTPOSTS.ERRORS.EMPTY_FIELDS'));
      return;
    }

    this.isCreating.set(true);
    this.errorMessage.set(null);

    this.projectService.createProjectPost(this.project.id, {
      title,
      content,
      contentFormat: this.contentFormat(),
      status: 'PUBLISHED'
    }).subscribe({
      next: (createdPost) => {
        this.posts.update((posts) => [createdPost, ...posts]);
        this.title.set('');
        this.content.set('');
        this.showCreateForm.set(false);
        this.isCreating.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translateService.instant('PROJECTPOSTS.ERRORS.CREATE'));
        this.isCreating.set(false);
      },
    });
  }

  setContentFormat(format: ProjectPostContentFormat): void {
    this.contentFormat.set(format);
  }

  insertMarkdown(prefix: string, suffix = '', placeholder = 'Text'): void {
    if (this.contentFormat() !== 'MARKDOWN') {
      return;
    }

    const textarea = this.postContentInput?.nativeElement;
    const value = this.content();

    const start = textarea?.selectionStart ?? value.length;
    const end = textarea?.selectionEnd ?? value.length;

    const selectedText = value.slice(start, end) || placeholder;

    const nextValue =
      value.slice(0, start) +
      prefix +
      selectedText +
      suffix +
      value.slice(end);

    this.content.set(nextValue);

    setTimeout(() => {
      textarea?.focus();

      const selectionStart = start + prefix.length;
      const selectionEnd = selectionStart + selectedText.length;

      textarea?.setSelectionRange(selectionStart, selectionEnd);
    });
  }

  insertMarkdownLine(prefix: string): void {
    if (this.contentFormat() !== 'MARKDOWN') {
      return;
    }

    const textarea = this.postContentInput?.nativeElement;
    const value = this.content();

    const start = textarea?.selectionStart ?? value.length;
    const before = value.slice(0, start);
    const after = value.slice(start);

    const needsNewLine = before.length > 0 && !before.endsWith('\n');
    const insertValue = `${needsNewLine ? '\n' : ''}${prefix}`;

    this.content.set(before + insertValue + after);

    setTimeout(() => {
      textarea?.focus();
      const cursorPosition = before.length + insertValue.length;
      textarea?.setSelectionRange(cursorPosition, cursorPosition);
    });
  }
}
