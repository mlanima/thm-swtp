import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, SimpleChanges, inject, signal, ElementRef, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, finalize, of, switchMap } from 'rxjs';
import { ProjectPostResponse, ProjectResponse } from '../../../../models/project.model';
import { ProjectService } from '../../project.service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { MarkdownPipe } from '../../../../shared/pipes/markdown.pipe';
import { SuccessModal } from '../../../../shared/success-modal/success-modal';

type ProjectPostContentFormat = 'PLAIN_TEXT' | 'MARKDOWN';

@Component({
  selector: 'app-project-posts',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe, MarkdownPipe, SuccessModal],
  templateUrl: './project-posts.html',
})

export class ProjectPosts implements OnChanges, OnDestroy {
  @Input({ required: true }) project!: ProjectResponse;
  @Input() canCreatePosts = false;

  private readonly projectService = inject(ProjectService);
  private readonly translateService = inject(TranslateService);
  private readonly maxImageSize = 5 * 1024 * 1024;
  private readonly allowedImageTypes = ['image/jpeg', 'image/png', 'image/webp'];

  posts = signal<ProjectPostResponse[]>([]);
  isLoading = signal(false);
  isCreating = signal(false);
  errorMessage = signal<string | null>(null);

  visibleCount = signal(3);

  title = signal('');
  content = signal('');
  showCreateForm = signal(false);

  contentFormat = signal<ProjectPostContentFormat>('MARKDOWN');
  deletingPostId = signal<string | null>(null);
  showDeleteSuccessModal = signal(false);

  selectedImage = signal<File | null>(null);
  imagePreviewUrl = signal<string | null>(null);
  postImageUrls = signal<Record<string, string>>({});

  @ViewChild('postContentInput')
  postContentInput?: ElementRef<HTMLTextAreaElement>;

  @ViewChild('postImageInput')
  postImageInput?: ElementRef<HTMLInputElement>;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['project'] && this.project?.id) {
      this.loadPosts();
    }
  }

  ngOnDestroy(): void {
    this.revokeImagePreviewUrl();
    this.clearPostImageUrls();
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
        this.clearPostImageUrls();
        this.posts.set(posts);
        this.visibleCount.set(3);
        this.loadPostImages(posts);
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
    const nextValue = !this.showCreateForm();

    this.showCreateForm.set(nextValue);
    this.errorMessage.set(null);

    if (!nextValue) { this.resetCreateForm(); }
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

    const image = this.selectedImage();

    this.projectService.createProjectPost(this.project.id, {
      title,
      content,
      contentFormat: this.contentFormat(),
      status: 'PUBLISHED',
    }).pipe(
      switchMap((createdPost) => {
        if (!image) {
          return of(createdPost);
        }

        return this.projectService.uploadProjectPostImage(
          this.project.id,
          createdPost.id,
          image
        ).pipe(
          catchError(() => {
            this.errorMessage.set(
              this.translateService.instant('PROJECTPOSTS.ERRORS.IMAGE_UPLOAD')
            );

            return of(createdPost);
          })
        );
      }),
      finalize(() => {
        this.isCreating.set(false);
      })
    ).subscribe({
      next: (createdOrUpdatedPost) => {
        this.posts.update((posts) => [createdOrUpdatedPost, ...posts]);

        if (createdOrUpdatedPost.imageUrl) {
          this.loadPostImage(createdOrUpdatedPost);
        }

        this.resetCreateForm();
        this.showCreateForm.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translateService.instant('PROJECTPOSTS.ERRORS.CREATE'));
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

  deletePost(postId: string): void {
    if (this.deletingPostId()) {
      return;
    }

    this.deletingPostId.set(postId);
    this.errorMessage.set(null);

    this.projectService.deleteProjectPost(this.project.id, postId).subscribe({
      next: () => {
        this.posts.update((posts) => posts.filter((post) => post.id !== postId));
        this.deletingPostId.set(null);
        this.showDeleteSuccessModal.set(true);
      },
      error: () => {
        this.errorMessage.set(
          this.translateService.instant('PROJECTPOSTS.ERRORS.DELETE')
        );
        this.deletingPostId.set(null);
      },
    });
  }

  closeDeleteSuccessModal(): void {
    this.showDeleteSuccessModal.set(false);
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      return;
    }

    if (!this.allowedImageTypes.includes(file.type)) {
      this.errorMessage.set(this.translateService.instant('PROJECTPOSTS.ERRORS.IMAGE_TYPE'));
      input.value = '';
      return;
    }

    if (file.size > this.maxImageSize) {
      this.errorMessage.set(this.translateService.instant('PROJECTPOSTS.ERRORS.IMAGE_SIZE'));
      input.value = '';
      return;
    }

    this.errorMessage.set(null);
    this.revokeImagePreviewUrl();

    this.selectedImage.set(file);
    this.imagePreviewUrl.set(URL.createObjectURL(file));
  }

  removeSelectedImage(): void {
    this.selectedImage.set(null);
    this.revokeImagePreviewUrl();

    if (this.postImageInput) {
      this.postImageInput.nativeElement.value = '';
    }
  }

  private resetCreateForm(): void {
    this.title.set('');
    this.content.set('');
    this.contentFormat.set('MARKDOWN');
    this.selectedImage.set(null);
    this.revokeImagePreviewUrl();

    if (this.postImageInput) {
      this.postImageInput.nativeElement.value = '';
    }
  }

  private revokeImagePreviewUrl(): void {
    const previewUrl = this.imagePreviewUrl();

    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
      this.imagePreviewUrl.set(null);
    }
  }

  private loadPostImages(posts: ProjectPostResponse[]): void {
    posts
      .filter((post) => !!post.imageUrl)
      .forEach((post) => this.loadPostImage(post));
  }

  private loadPostImage(post: ProjectPostResponse): void {
    this.projectService.getProjectPostImage(this.project.id, post.id).subscribe({
      next: (blob) => {
        const objectUrl = URL.createObjectURL(blob);

        this.postImageUrls.update((urls) => {
          const oldUrl = urls[post.id];

          if (oldUrl) {
            URL.revokeObjectURL(oldUrl);
          }

          return {
            ...urls,
            [post.id]: objectUrl,
          };
        });
      },
      error: () => {
        this.postImageUrls.update((urls) => {
          const { [post.id]: removed, ...remainingUrls } = urls;
          return remainingUrls;
        });
      },
    });
  }

  private clearPostImageUrls(): void {
    Object.values(this.postImageUrls()).forEach((url) => {
      URL.revokeObjectURL(url);
    });

    this.postImageUrls.set({});
  }
}
