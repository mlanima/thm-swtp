import { Component, Input, OnChanges, OnInit, SimpleChanges, inject, signal } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { EditableTagListComponent } from '../../../../shared/tags/tag-list/editable-tag-list.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { UserProfileTagService, TagResponse } from '../../services/user-profile-tag.service'

@Component({
  selector: 'app-profile-tag-list',
  standalone: true,
  imports: [EditableTagListComponent, TranslatePipe],
  templateUrl: './profile-tag-list.component.html',
})
export class ProfileTagListComponent implements OnInit, OnChanges {
  private readonly userProfileTagService = inject(UserProfileTagService);
  private readonly translateService = inject(TranslateService);
  private readonly toastService = inject(ToastService);

  @Input({ required: true }) username?: string;
  @Input() isOwner = false;

  tags = signal<TagResponse[]>([]);
  isLoading = signal(false);
  isSaving = signal(false);
  isDeleting = signal(false);

  ngOnInit(): void {
    this.loadTags();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['username'] && !changes['username'].firstChange) {
      this.loadTags();
    }
  }

  saveTag(name: string): void {
    const username = this.username;
    const cleanedName = name.trim();

    if (!username || !cleanedName || !this.isOwner || this.isSaving()) {
      return;
    }

    this.isSaving.set(true);

    this.userProfileTagService.addTag(username, { name: cleanedName }).subscribe({
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
    const username = this.username;

    if (!username || !this.isOwner || this.isDeleting()) {
      return;
    }

    this.isDeleting.set(true);

    this.userProfileTagService.deleteTag(tagName).subscribe({
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
    if (!this.username) {
      return;
    }

    this.isLoading.set(true);

    this.userProfileTagService.getProfileTags(this.username).subscribe({
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
