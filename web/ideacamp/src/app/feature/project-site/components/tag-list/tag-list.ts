import { Component, Input, OnChanges, OnInit, SimpleChanges, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ProjectTagService, TagResponse } from '../../services/project-tag.service';
import { EditableTagListComponent } from '../../../../shared/tags/tag-list/editable-tag-list.component';
import { ToastService } from '../../../../shared/toast/toast.service';

@Component({
  selector: 'app-tag-list',
  standalone: true,
  imports: [EditableTagListComponent],
  templateUrl: './tag-list.html',
})
export class TagList implements OnInit, OnChanges {
  private readonly projectTagService = inject(ProjectTagService);
  private static readonly TAG_PATTERN = /^[a-zA-Z0-9äöüÄÖÜß \-.]+$/;
  private readonly translateService = inject(TranslateService);
  private readonly toastService = inject(ToastService);

  @Input({ required: true }) projectId?: string;
  @Input() isOwner = false;

  tags = signal<TagResponse[]>([]);
  isLoading = signal(false);
  isSaving = signal(false);
  isDeleting = signal(false);

  ngOnInit(): void {
    this.loadTags();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['projectId'] && !changes['projectId'].firstChange) {
      this.loadTags();
    }
  }

  saveTag(name: string): void {
    const projectId = this.projectId;
    const cleanedName = name.trim();

    if (!projectId || !cleanedName) {
      return;
    }

    if (!TagList.TAG_PATTERN.test(name)) {
      this.toastService.warning(this.translateService.instant('PROJECTSITE.TAGS.ERROR_INVALID_CHARS'));
      return;
    }

    this.isSaving.set(true);

    this.projectTagService.addTag(projectId, { name: cleanedName }).subscribe({
      next: (tag) => {
        const lower = tag.name.toLowerCase();
        const existing = this.tags().some((item) => item.name.toLowerCase() === lower);

        if (existing) {
          this.toastService.warning(this.translateService.instant('PROJECTSITE.TAGS.ERROR_DUPLICATE'));
        }

        if (!existing) {
          this.tags.set([...this.tags(), tag]);
        }
        this.isSaving.set(false);
      },
      error: (err) => {
        const apiError = err.error as { errorCode?: string };
        if (err.status === 400 && apiError?.errorCode === 'TAG_NOT_VALID') {
          this.toastService.error(
            this.translateService.instant('PROJECTSITE.TAGS.ERROR_NOT_VALID', { name: cleanedName })
          );
        } else if (err.status === 502) {
          this.toastService.error(this.translateService.instant('PROJECTSITE.TAGS.ERROR_VALIDATION'));
        } else {
          this.toastService.error(this.translateService.instant('PROJECTSITE.TAGS.ERROR_TOO_LONG'));
        }
        this.isSaving.set(false);
      },
    });
  }

  deleteTag(tagName: string): void {
    const projectId = this.projectId;
    if (!projectId || !this.isOwner) return;

    this.isDeleting.set(true);

    this.projectTagService.deleteTag(projectId, tagName).subscribe({
      next: () => {
        const lower = tagName.toLowerCase();
        this.tags.set(this.tags().filter((tag) => tag.name.toLowerCase() !== lower));
        this.isDeleting.set(false);
      },
      error: () => {
        this.toastService.error(this.translateService.instant('PROJECTSITE.TAGS.ERROR_DELETE'));
        this.isDeleting.set(false);
      },
    });
  }

  private loadTags(): void {
    if (!this.projectId) return;
    this.isLoading.set(true);

    this.projectTagService.getProjectTags(this.projectId).subscribe({
      next: (tags) => {
        this.tags.set(tags);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error(this.translateService.instant('PROJECTSITE.TAGS.ERROR_LOAD'));
        this.isLoading.set(false);
      },
    });
  }
}
